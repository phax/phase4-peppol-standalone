/*
 * Copyright (C) 2023-204 Philip Helger (www.helger.com)
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
package com.mysupply.phase4.peppolstandalone.spi;

import javax.annotation.Nonnull;

import com.helger.security.certificate.CertificateHelper;
import com.mysupply.phase4.domain.Document;
import com.mysupply.phase4.peppolstandalone.APConfig;
import com.mysupply.phase4.peppolstandalone.context.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.sbdh.PeppolSBDHData;
//import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;
import com.mysupply.phase4.persistence.ISBDRepository;

/**
 * This is a way of handling incoming Peppol messages
 *
 * @author Philip Helger
 */
@IsSPIImplementation
@Component
@Configurable
public class PeppolIncomingSBDHandlerSPI implements IPhase4PeppolIncomingSBDHandlerSPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolIncomingSBDHandlerSPI.class);

    @Autowired
    private ISBDRepository sbdRepository;

    public PeppolIncomingSBDHandlerSPI() {
        SpringContextHolder.autowireBean(this);
    }

    public void handleIncomingSBD(@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                  @Nonnull final HttpHeaderMap aHeaders,
                                  @Nonnull final Ebms3UserMessage aUserMessage,
                                  @Nonnull final byte[] aSBDBytes,
                                  @Nonnull final StandardBusinessDocument aSBD,
                                  @Nonnull final PeppolSBDHData aPeppolSBD,
                                  @Nonnull final IAS4IncomingMessageState aIncomingState,
                                  @Nonnull final ICommonsList<Ebms3Error> aProcessingErrorMessages) throws Exception {

        final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();
        try {
            // Example code snippets how to get data
            String c1 = aPeppolSBD.getSenderAsIdentifier().getURIEncoded();
            String c2 = CertificateHelper.getPEMEncodedCertificate (aIncomingState.getSigningCertificate ());

            String c4 = aPeppolSBD.getReceiverAsIdentifier().getURIEncoded();
            String docType = aPeppolSBD.getDocumentTypeAsIdentifier().getURIEncoded();
            String process = aPeppolSBD.getProcessAsIdentifier().getURIEncoded();
            String countryC1 = aPeppolSBD.getCountryC1();

            LOGGER.info("Received a new Peppol Message");
            LOGGER.info("  C1 = " + c1);
            LOGGER.info("  C2 = " + c2);
            LOGGER.info("  C3 = " + sMyPeppolSeatID);
            LOGGER.info("  C4 = " + c4);
            LOGGER.info("  DocType = " + docType);
            LOGGER.info("  Process = " + process);
            LOGGER.info("  CountryC1 = " + c1);
            LOGGER.info("  CountryC2 = " + countryC1);
            LOGGER.info("  CountryC4 = " + c4);
        } catch (NullPointerException ex) {
            LOGGER.error("An error occurred.", ex);
        }

        try {
            Document documentToStore = new Document(aSBDBytes);
            this.sbdRepository.save(documentToStore);
            LOGGER.info("SBD saved successfully");
        } catch (Exception ex) {
            LOGGER.error("Failed to save SBD", ex);
            throw new Exception("Failed to save SBD");
        }

        new Thread(() -> {
            final boolean createPeppolReportingItem = AS4Configuration
                    .getConfig()
                    .getAsBoolean("peppol.createReportingItem");

            if (createPeppolReportingItem)
                try {
                    LOGGER.info("Creating Peppol Reporting Item and storing it");

                    // TODO determine correct values
                    final String sC3ID = aPeppolSBD.getReceiverAsIdentifier().getURIEncoded(); //mySupply is located in Denmark, we hardcode DK
                    final String sC4CountryCode = "DK";
                    final String sEndUserID = "EndUserID";
                    final PeppolReportingItem aReportingItem = Phase4PeppolServletMessageProcessorSPI.createPeppolReportingItemForReceivedMessage(aUserMessage,
                            aPeppolSBD,
                            aIncomingState,
                            sC3ID,
                            sC4CountryCode,
                            sEndUserID);

                    PeppolReportingBackend.withBackendDo(APConfig.getConfig(),
                            aBackend -> aBackend.storeReportingItem(aReportingItem));
                } catch (final PeppolReportingBackendException ex) {
                    LOGGER.error("Failed to store Peppol Reporting Item", ex);
                    // TODO improve error handling
                }
        }).start();
    }

    @Autowired
    private void setSbdRepository(ISBDRepository sbdRepository) {
        this.sbdRepository = sbdRepository;
    }
}
