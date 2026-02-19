package com.mysupply.phase4.peppolstandalone.reporting;

import java.time.YearMonth;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import org.slf4j.Logger;
import com.helger.phase4.logging.Phase4LoggerFactory;

/**
 * Console application to create an End User Statistics Report.
 */
public class EndUserStatisticsReportApp
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (EndUserStatisticsReportApp.class);

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

    LOGGER.info ("Creating End User Statistics Report for " + aYearMonth);

    try
    {
      final EndUserStatisticsReportType aReport = AppReportingHelper.createEUSR (aYearMonth);
      if (aReport != null)
      {
        LOGGER.info ("Successfully created End User Statistics Report for " + aYearMonth);
        LOGGER.info ("Number of reporting items: " + aReport.getSubsetCount());
      }
      else
      {
        LOGGER.error ("Failed to create End User Statistics Report for " + aYearMonth);
      }
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error creating End User Statistics Report", ex);
    }
  }
}
