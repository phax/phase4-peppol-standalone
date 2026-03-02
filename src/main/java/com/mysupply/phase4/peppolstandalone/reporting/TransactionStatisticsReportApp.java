package com.mysupply.phase4.peppolstandalone.reporting;

import com.helger.base.wrapper.Wrapper;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppol.reportingsupport.EPeppolReportType;
import com.helger.peppol.reportingsupport.IPeppolReportSenderCallback;
import com.helger.peppol.reportingsupport.IPeppolReportStorage;
import com.helger.peppol.reportingsupport.PeppolReportingSupport;
import com.helger.peppol.reportingsupport.sql.PeppolReportSQLHandler;
import com.helger.peppol.reportingsupport.sql.PeppolReportStorageSQL;
import com.helger.phase4.logging.Phase4LoggerFactory;
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
 * Console application to create a Transaction Statistics Report.
 */
@SpringBootApplication
@ComponentScan(
        basePackages = {"com.mysupply.phase4", "com.helger"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.mysupply\\.phase4\\.peppolstandalone\\.reporting\\.EndUserStatisticsReportApp"
        )
)
public class TransactionStatisticsReportApp implements CommandLineRunner {
    private static final Logger LOGGER = Phase4LoggerFactory.getLogger(TransactionStatisticsReportApp.class);

    public static void main(final String[] args) {
        SpringApplication app = new SpringApplication(TransactionStatisticsReportApp.class);
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

        LOGGER.info("Creating Transaction Statistics Report for " + yearMonth);

        try {
            final TransactionStatisticsReportType report = AppReportingHelper.createTSR(yearMonth);
            if (report != null) {
                LOGGER.info("Successfully created Transaction Statistics Report for " + yearMonth);
                LOGGER.info("Number of reporting items: " + report.getSubtotalCount());

                final IPeppolReportSenderCallback reportSender = AppReportingHelper.getPeppolReportSender();
                try (final PeppolReportSQLHandler aSQLHandler = new PeppolReportSQLHandler(APConfig.getConfig())) {
                    final IPeppolReportStorage aReportingStorage = new PeppolReportStorageSQL(aSQLHandler, aSQLHandler.getTableNamePrefix());
                    final PeppolReportingSupport aPeppolReportingSupport = new PeppolReportingSupport(aReportingStorage);
                    final Wrapper<String> aTSRString = new Wrapper<>();
                    if (aPeppolReportingSupport.validateAndStorePeppolTSR10(report, aTSRString::set).isSuccess()) {
                        final String sTSRXml = aTSRString.get();
                        if (sTSRXml == null) {
                            LOGGER.error("TSR XML string was null after successful validation for " + yearMonth);
                        } else if (aPeppolReportingSupport.sendPeppolReport(yearMonth, EPeppolReportType.TSR_V10, sTSRXml, reportSender).isSuccess()) {
                            LOGGER.info("Successfully sent Transaction Statistics Report for " + yearMonth);
                        } else {
                            LOGGER.error("Failed to send Transaction Statistics Report for " + yearMonth);
                        }
                    } else {
                        LOGGER.error("Failed to validate and store Transaction Statistics Report for " + yearMonth);
                    }
                }
            } else {
                LOGGER.error("Failed to create Transaction Statistics Report for " + yearMonth);
            }
        } catch (final Exception ex) {
            LOGGER.error("Error creating Transaction Statistics Report", ex);
        }
    }
}
