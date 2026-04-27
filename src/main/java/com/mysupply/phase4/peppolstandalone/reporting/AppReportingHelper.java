/*
 * Copyright (C) 2023-2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysupply.phase4.peppolstandalone.reporting;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;

import com.helger.base.string.StringHelper;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reportingsupport.IPeppolReportSenderCallback;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.eusr.EndUserStatisticsReport;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppol.reporting.tsr.TransactionStatisticsReport;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.mysupply.phase4.peppolstandalone.APConfig;
import com.mysupply.phase4.peppolstandalone.controller.PeppolSender;
import com.mysupply.phase4.peppolstandalone.controller.HttpForbiddenException;
import com.helger.security.certificate.TrustedCAChecker;

/**
 * Helper class for report generation
 *
 * @author Philip Helger
 */
public final class AppReportingHelper {
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger(AppReportingHelper.class);

  @NonNull
  public static YearMonth getValidYearMonthInAPI(final int nYear, final int nMonth) {
    if (nYear < 2024)
      throw new HttpForbiddenException("The year value " + nYear + " is too low");
    if (nMonth < 1 || nMonth > 12)
      throw new HttpForbiddenException("The month value " + nMonth + " is invalid");

    final LocalDate aNow = PDTFactory.getCurrentLocalDate();
    if (nYear > aNow.getYear())
      throw new HttpForbiddenException("The year value " + nYear + " is in the future");
    if (nYear == aNow.getYear() && nMonth > aNow.getMonthValue())
      throw new HttpForbiddenException("The month value " + nMonth + " is in the future");

    return YearMonth.of(nYear, nMonth);
  }

  public static TransactionStatisticsReportType createTSR(@NonNull final YearMonth aYearMonth) throws PeppolReportingBackendException, IllegalStateException {
    LOGGER.info("Trying to create Peppol Reporting TSR for " + aYearMonth);

    // Now get all items from data storage and store them in a list (we start
    // with an initial size of 1K to avoid too many copy operations)
    final ICommonsList<PeppolReportingItem> aReportingItems = new CommonsArrayList<>(1024);
    if (PeppolReportingBackend.withBackendDo(APConfig.getConfig(),
                    aBackend -> aBackend.forEachReportingItem(aYearMonth,
                            aReportingItems::add))
            .isSuccess()) {
      // Create report with the read transactions
      return TransactionStatisticsReport.builder()
              .monthOf(aYearMonth)
              .reportingServiceProviderID(APConfig.getMyPeppolSeatID())
              .reportingItemList(aReportingItems)
              .build();
    }

    throw new IllegalStateException("Failed to build Transaction Statistics Report.");
  }

  public static EndUserStatisticsReportType createEUSR(@NonNull final YearMonth aYearMonth) throws PeppolReportingBackendException, IllegalStateException {
    LOGGER.info("Trying to create Peppol Reporting EUSR for " + aYearMonth);

    // Now get all items from data storage and store them in a list (we start
    // with an initial size of 1K to avoid too many copy operations)
    final ICommonsList<PeppolReportingItem> aReportingItems = new CommonsArrayList<>(1024);
    if (PeppolReportingBackend.withBackendDo(APConfig.getConfig(),
                    aBackend -> aBackend.forEachReportingItem(aYearMonth,
                            aReportingItems::add))
            .isSuccess()) {
      // Create report with the read transactions
      return EndUserStatisticsReport.builder()
              .monthOf(aYearMonth)
              .reportingServiceProviderID(APConfig.getMyPeppolSeatID())
              .reportingItemList(aReportingItems)
              .build();
    }

    throw new IllegalStateException("Failed to build End User Statistics Report.");
  }

  public static IPeppolReportSenderCallback getPeppolReportSender() {
    return (aDocTypeID, aProcessID, sMessagePayload) -> {
      // Make Network decisions
      final EPeppolNetwork eStage = APConfig.getPeppolStage();
      final ISMLInfo aSMLInfo = eStage.getSMLInfo();
      final TrustedCAChecker aAPCA = eStage.isProduction() ? PeppolTrustedCA.peppolProductionAP()
              : PeppolTrustedCA.peppolTestAP();
      // Sender: your company participant ID
      final String sSenderID = APConfig.getMyPeppolReportingSenderID();
      if (StringHelper.isEmpty(sSenderID))
        throw new IllegalStateException("No Peppol Reporting Sender ID is configured");

      // Receiver: production OpenPeppol; test Helger
      // OpenPeppol doesn't offer this participant ID on test :-/
      final String sReceiverID = eStage.isProduction() ? CPeppolReporting.OPENPEPPOL_PARTICIPANT_ID : "9915:helger";

      final String sCountryC1 = APConfig.getMyPeppolCountryCode();
      if (!PeppolReportingHelper.isValidCountryCode(sCountryC1))
        throw new IllegalStateException("Invalid country code of Peppol owner is defined: '" + sCountryC1 + "'");

      // Returns the sending report
      final Phase4PeppolSendingReport aSendingReport = PeppolSender.sendPeppolMessageCreatingSbdh(aSMLInfo,
              aAPCA,
              sMessagePayload.getBytes(StandardCharsets.UTF_8),
              sSenderID,
              sReceiverID,
              aDocTypeID.getURIEncoded(),
              aProcessID.getURIEncoded(),
              sCountryC1);
      return aSendingReport.getAsXMLString();
    };
  }
}
