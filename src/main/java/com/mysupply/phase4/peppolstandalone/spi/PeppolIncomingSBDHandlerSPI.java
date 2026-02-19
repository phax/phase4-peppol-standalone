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

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.http.header.HttpHeaderMap;
import com.helger.security.certificate.CertificateHelper;
import com.mysupply.phase4.ICountryCodeMapper;
import com.mysupply.phase4.domain.Document;
import com.mysupply.phase4.peppolstandalone.APConfig;
import com.mysupply.phase4.peppolstandalone.context.SpringContextHolder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4ErrorList;
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

    private ISBDRepository sbdRepository;

    private ICountryCodeMapper countryCodeMapper;

    private boolean isAutowired = false;

    public PeppolIncomingSBDHandlerSPI() {
        // Don't autowire in constructor - context may not be ready yet
    }

    private void ensureAutowired() {
        if (!isAutowired) {
            SpringContextHolder.autowireBean(this);
            isAutowired = true;
        }
    }

    public void handleIncomingSBD(@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                  @NonNull final HttpHeaderMap aHeaders,
                                  @NonNull final Ebms3UserMessage aUserMessage,
                                  @NonNull final byte [] aSBDBytes,
                                  @NonNull final StandardBusinessDocument aSBD,
                                  @NonNull final PeppolSBDHData aPeppolSBD,
                                  @NonNull final IAS4IncomingMessageState aIncomingState,
                                  @NonNull final AS4ErrorList aProcessingErrorMessages) throws Exception {

        // Ensure dependencies are autowired before use
        ensureAutowired();

        final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();
        try {
            // Example code snippets how to get data
            String c1 = aPeppolSBD.getSenderAsIdentifier().getURIEncoded();
            String c2 = CertificateHelper.getPEMEncodedCertificate (aIncomingState.getSigningCertificate ());
            String c4 = aPeppolSBD.getReceiverAsIdentifier().getURIEncoded();
            String docType = aPeppolSBD.getDocumentTypeAsIdentifier().getURIEncoded();
            String process = aPeppolSBD.getProcessAsIdentifier().getURIEncoded();
            String countryC1 = aPeppolSBD.getCountryC1();

            StringBuilder sb = new StringBuilder();
            sb.append("Received a new Peppol Message").append(System.lineSeparator())
                .append("  C1 = ").append(c1).append(System.lineSeparator())
                .append("  C2 = ").append(c2).append(System.lineSeparator())
                .append("  C3 = ").append(sMyPeppolSeatID).append(System.lineSeparator())
                .append("  C4 = ").append(c4).append(System.lineSeparator())
                .append("  DocType = ").append(docType).append(System.lineSeparator())
                .append("  Process = ").append(process).append(System.lineSeparator())
                .append("  CountryC1 = ").append(c1).append(System.lineSeparator())
                .append("  CountryC2 = ").append(countryC1).append(System.lineSeparator())
                .append("  CountryC4 = ").append(c4);
            LOGGER.info(sb.toString());
        } catch (NullPointerException ex) {
            LOGGER.error("An error occurred.", ex);
        }

        try {
            String c1 = aPeppolSBD.getSenderAsIdentifier().getURIEncoded();
            String c4 = aPeppolSBD.getReceiverAsIdentifier().getURIEncoded();
            String docType = aPeppolSBD.getDocumentTypeAsIdentifier().getURIEncoded();
            String process = aPeppolSBD.getProcessAsIdentifier().getURIEncoded();
            String domain = this.GetDomain(aHeaders);
            String messageID = aUserMessage.getMessageInfo().getMessageId();
            String conversationID = aUserMessage.getCollaborationInfo().getConversationId(); //
            String senderCertificate = aIncomingState.getSigningCertificate() != null
                ? aIncomingState.getSigningCertificate().getSubjectX500Principal().getName()
                : null;
            String receiverCertificate = aIncomingState.getDecryptingCertificate() != null
                ? aIncomingState.getDecryptingCertificate().getSubjectX500Principal().getName()
                : null;
            String protocol = "peppol-transport-as4-v2_0";// aMessageMetadataaMessageMetadata is of type IAS4IncomingMessageMetadata, so we just hardcode implement information. //

            String f = getEndpointUrl(aHeaders);

            Document documentToStore = Document.builder()
                .data(aSBDBytes)
                .domain(domain)
                .senderIdentifier(c1)
                .receiverIdentifier(c4)
                .docType(docType)
                .process(process)
                .messageId(messageID)
                .conversationId(conversationID)
                .senderCertificate(senderCertificate)
                .receiverCertificate(receiverCertificate)
                .protocol(protocol)
                .dataSize(aSBDBytes.length)
                .build();

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
                    final String sC3ID = aPeppolSBD.getReceiverAsIdentifier().getURIEncoded();
                    final String sC4CountryCode = this.countryCodeMapper.mapCountryCode(aPeppolSBD.getReceiverScheme(), aPeppolSBD.getReceiverValue()) ;// "DK"; // incorrect, we need to determine the country code like in VAX
                    final String sEndUserID = APConfig.getMyPeppolSeatID ();
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

    private String GetDomain(HttpHeaderMap aHeaders)
    {
        String forwardedHost = aHeaders.getFirstHeaderValue("X-Forwarded-Host");
        if (forwardedHost != null) {
            return forwardedHost;
        }

        String host = aHeaders.getFirstHeaderValue("host");
        if (host != null) {
            if (host.contains(":"))
            {
                host = host.substring(0, host.indexOf(':'));
            }

            return host;
        }

        return "?";
    }

    /**
     * Gets the full endpoint URL that was called by the sender.
     * Supports reverse proxy scenarios with X-Forwarded-* headers.
     */
    private String getEndpointUrl(HttpHeaderMap aHeaders) {
        // Check for reverse proxy headers first
        String host = aHeaders.getFirstHeaderValue("X-Forwarded-Host");
        if (host == null) {
            host = aHeaders.getFirstHeaderValue("host");
        }

        String proto = aHeaders.getFirstHeaderValue("X-Forwarded-Proto");
        if (proto == null) {
            proto = "https"; // Default to https for Peppol
        }

        String path = aHeaders.getFirstHeaderValue("X-Forwarded-Path");
        if (path == null) {
            path = "/as4"; // Default AS4 endpoint path
        }

        if (host != null) {
            return proto + "://" + host + path;
        }

        return null;
    }

    @Autowired
    private void setSbdRepository(ISBDRepository sbdRepository) {
        this.sbdRepository = sbdRepository;
    }

    @Autowired
    private void setCountryCodeMapper(ICountryCodeMapper countryCodeMapper) {
        this.countryCodeMapper = countryCodeMapper;
    }
}
