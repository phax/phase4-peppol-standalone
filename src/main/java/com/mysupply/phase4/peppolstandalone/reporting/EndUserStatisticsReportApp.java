package com.mysupply.phase4.peppolstandalone.reporting;

import com.helger.base.wrapper.Wrapper;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.eusr.v110.SubsetKeyType;
import com.helger.peppol.reportingsupport.EPeppolReportType;
import com.helger.peppol.reportingsupport.IPeppolReportSenderCallback;
import com.helger.peppol.reportingsupport.IPeppolReportStorage;
import com.helger.peppol.reportingsupport.PeppolReportingSupport;
import com.helger.peppol.reportingsupport.sql.PeppolReportSQLHandler;
import com.helger.peppol.reportingsupport.sql.PeppolReportStorageSQL;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.mysupply.phase4.CountryCodeMapper;
import com.mysupply.phase4.peppolstandalone.APConfig;
import org.slf4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.time.YearMonth;

/**
 * Console application to create an End User Statistics Report.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.mysupply.phase4", "com.helger"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.mysupply\\.phase4\\.peppolstandalone\\.reporting\\.TransactionStatisticsReportApp"
        )
)
public class EndUserStatisticsReportApp implements CommandLineRunner {
    private static final Logger LOGGER = Phase4LoggerFactory.getLogger(EndUserStatisticsReportApp.class);

    private static final String COUNTRY_CODE_META_SCHEME_ID = "CC";

    public static void main(final String[] args) {
        SpringApplication app = new SpringApplication(EndUserStatisticsReportApp.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        YearMonth yearMonth;
        if (args.length == 2) {
            yearMonth = YearMonth.of(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } else {
            yearMonth = YearMonth.now().minusMonths(1);
        }

        LOGGER.info("Creating End User Statistics Report for " + yearMonth);

        try {
            final EndUserStatisticsReportType report = AppReportingHelper.createEUSR(yearMonth);
            if (report != null) {
                LOGGER.info("Successfully created End User Statistics Report for " + yearMonth);
                LOGGER.info("Number of reporting items: " + report.getSubsetCount());

                final IPeppolReportSenderCallback reportSender = AppReportingHelper.getPeppolReportSender();
                try (final PeppolReportSQLHandler aSQLHandler = new PeppolReportSQLHandler(APConfig.getConfig())) {
                    final IPeppolReportStorage aReportingStorage = new PeppolReportStorageSQL(aSQLHandler, aSQLHandler.getTableNamePrefix());
                    final PeppolReportingSupport aPRS = new PeppolReportingSupport(aReportingStorage);
                    final Wrapper<String> aEUSRString = new Wrapper<>();

                    this.cleanUpEndUserStatisticsReport(report);

                    if (aPRS.validateAndStorePeppolEUSR11(report, aEUSRString::set).isSuccess()) {
                        final String sEUSRXml = aEUSRString.get();
                        if (sEUSRXml == null) {
                            LOGGER.error("EUSR XML string was null after successful validation for " + yearMonth);
                        } else if (aPRS.sendPeppolReport(yearMonth, EPeppolReportType.EUSR_V11, sEUSRXml, reportSender).isSuccess()) {
                            LOGGER.info("Successfully sent End User Statistics Report for " + yearMonth);
                        } else {
                            LOGGER.error("Failed to send End User Statistics Report for " + yearMonth);
                        }
                    } else {
                        LOGGER.error("Failed to validate and store End User Statistics Report for " + yearMonth);
                    }
                }
            } else {
                LOGGER.error("Failed to create End User Statistics Report for " + yearMonth);
            }
        } catch (final Exception ex) {
            LOGGER.error("Error creating End User Statistics Report", ex);
        }
    }

    private void cleanUpEndUserStatisticsReport(final EndUserStatisticsReportType report) {
        report.getSubset().removeIf(subsetType ->
                subsetType.getKey().stream().anyMatch(keyType ->
                        this.isDefaultCountryCode(keyType)));
    }

    private boolean isDefaultCountryCode(final SubsetKeyType subset) {
        boolean isDefaultCountryCode = subset.getMetaSchemeID().equals(COUNTRY_CODE_META_SCHEME_ID) && subset.getValue().equals(CountryCodeMapper.DEFAULT_COUNTRY_CODE);
        return isDefaultCountryCode;
    }
}
