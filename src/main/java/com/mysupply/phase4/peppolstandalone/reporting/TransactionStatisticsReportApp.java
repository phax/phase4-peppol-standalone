package com.mysupply.phase4.peppolstandalone.reporting;

import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.phase4.logging.Phase4LoggerFactory;
import org.slf4j.Logger;
import java.time.YearMonth;

/**
 * Console application to create a Transaction Statistics Report.
 */
public class TransactionStatisticsReportApp {
    private static final Logger LOGGER = Phase4LoggerFactory.getLogger (TransactionStatisticsReportApp.class);

    public static void main (final String [] args)
    {
        YearMonth aYearMonth;
        if (args.length == 2)
        {
            aYearMonth = YearMonth.of (Integer.parseInt (args[0]), Integer.parseInt (args[1]));
        }
        else
        {
            aYearMonth = YearMonth.now ().minusMonths (1);
        }

        LOGGER.info ("Creation Transaction Statistics Report for " + aYearMonth);

        try
        {
            final TransactionStatisticsReportType aReport = AppReportingHelper.createTSR (aYearMonth);
            if (aReport != null)
            {
                LOGGER.info ("Successfully created Transaction Statistics Report for " + aYearMonth);
                LOGGER.info ("Number of reporting items: " + aReport.getSubtotalCount());
            }
            else
            {
                LOGGER.error ("Failed to create Transaction Statistics Report for " + aYearMonth);
            }
        }
        catch (final Exception ex)
        {
            LOGGER.error ("Error creating Transaction Statistics Report", ex);
        }
    }
}
