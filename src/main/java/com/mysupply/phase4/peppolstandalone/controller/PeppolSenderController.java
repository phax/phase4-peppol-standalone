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
package com.mysupply.phase4.peppolstandalone.controller;

import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.smpclient.peppol.SMPClient;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.mysupply.phase4.ICountryCodeMapper;
import com.mysupply.phase4.peppolstandalone.dto.Phase4PeppolSendingReportExtension;
import com.mysupply.phase4.persistence.ISBDRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.string.StringHelper;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.mysupply.phase4.peppolstandalone.APConfig;

import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sml.ESML;
import com.helger.peppolid.factory.PeppolIdentifierFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@RestController
@RequestMapping("/sender/v1.0")
public class PeppolSenderController {
    static final String HEADER_X_TOKEN = "X-Token";
    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolSenderController.class);
    private static final LocalDateTime ONLINE_TIMESTAMP = LocalDateTime.now();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ICountryCodeMapper countryCodeMapper;

    @Autowired
    private ISBDRepository sbdRepository;

    @Autowired
    private void setSbdRepository(ISBDRepository sbdRepository) {
        this.sbdRepository = sbdRepository;
    }

    @Autowired
    private void setCountryCodeMapper(ICountryCodeMapper countryCodeMapper) {
        this.countryCodeMapper = countryCodeMapper;
    }

    @GetMapping(path = "/online", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> online() {
        // It is a post method, so it can be used from a browser or monitoring tool to check if the service is online.
        return ResponseEntity
                .ok(ONLINE_TIMESTAMP.format(FORMATTER));
    }


    @GetMapping(path = "/logonCheck", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> logonCheck(@RequestHeader(HEADER_X_TOKEN) final String xtoken) {
        ResponseEntity<String> errorResponse = this.validateToken(xtoken);
        if (errorResponse != null)
        {
            return errorResponse;
        }

        return ResponseEntity.ok("OK");
    }



    @PostMapping(path = "/communicationCheck", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> communicationCheck(@RequestHeader(HEADER_X_TOKEN) final String xtoken,
                                        @RequestBody final byte[] aPayloadBytes)
    {
        ResponseEntity<String> errorResponse = this.validateToken(xtoken);
        if (errorResponse != null)
        {
            return errorResponse;
        }

        final EPeppolNetwork eStage = APConfig.getPeppolStage();
        final ESML eSML = eStage.isProduction() ? ESML.DIGIT_PRODUCTION : ESML.DIGIT_TEST;
        //final Phase4PeppolSendingReport aSendingReport = new Phase4PeppolSendingReport(eSML);

        final PeppolSBDHData aData;
        try
        {
            aData = new PeppolSBDHDataReader(PeppolIdentifierFactory.INSTANCE).extractData(new NonBlockingByteArrayInputStream(aPayloadBytes));
        }
        catch (final PeppolSBDHDataReadException ex)
        {
            LOGGER.error("The received data is not valid: ", ex);
            return ResponseEntity
                    .badRequest()
                    .body("The received data is not understood");
        }

        String senderValue = aData.getSenderValue();
        if (!"0000:0000".equalsIgnoreCase(senderValue))
        {
            return ResponseEntity
                    .badRequest()
                    .body("The received data is not valid, as sender value is incorrect. Expected 'æøå' but got '" + senderValue + "'");
        }

        return ResponseEntity.ok("OK");
    }


    @GetMapping(path = "/mapEndpointToCountryCode", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String>  mapEndpointToCountryCode(@RequestHeader(HEADER_X_TOKEN) final String xtoken,
                                           @RequestBody final String endpoint) {
        ResponseEntity<String> errorResponse = this.validateToken(xtoken);
        if (errorResponse != null)
        {
            return errorResponse;
        }

        return ResponseEntity.ok(this.countryCodeMapper.mapCountryCode(endpoint));
    }

    @PostMapping(path = "/send", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> sendPeppolSbdhMessage(@RequestHeader(HEADER_X_TOKEN) final String xtoken,
                                        @RequestBody final byte[] aPayloadBytes)
    {
        ResponseEntity<String> errorResponse = this.validateToken(xtoken);
        if (errorResponse != null)
        {
            return errorResponse;
        }

        final EPeppolNetwork eStage = APConfig.getPeppolStage();
        final ESML eSML = eStage.isProduction() ? ESML.DIGIT_PRODUCTION : ESML.DIGIT_TEST;
        final TrustedCAChecker aApCaChecker = eStage.isProduction () ? PeppolTrustedCA.peppolProductionAP () : PeppolTrustedCA.peppolTestAP();

        final Phase4PeppolSendingReportExtension aSendingReport = new Phase4PeppolSendingReportExtension(eSML);

        final PeppolSBDHData aData;
        try
        {
            aData = new PeppolSBDHDataReader(PeppolIdentifierFactory.INSTANCE).extractData(new NonBlockingByteArrayInputStream(aPayloadBytes));
        }
        catch (final PeppolSBDHDataReadException ex)
        {
            aSendingReport.setSBDHParseException(ex);
            aSendingReport.setSendingSuccess(false);
            aSendingReport.setOverallSuccess(false);
            return ResponseEntity.badRequest().body(aSendingReport.getAsJsonString());
        }

        aSendingReport.setSenderID(aData.getSenderAsIdentifier());
        aSendingReport.setReceiverID(aData.getReceiverAsIdentifier());
        aSendingReport.setDocTypeID(aData.getDocumentTypeAsIdentifier());
        aSendingReport.setProcessID(aData.getProcessAsIdentifier());
        aSendingReport.setCountryC1(aData.getCountryC1());
        aSendingReport.setSBDHInstanceIdentifier(aData.getInstanceIdentifier());

        final String sSenderID = aData.getSenderAsIdentifier() != null ? aData.getSenderAsIdentifier().getURIEncoded() : "";
        final String sReceiverID = aData.getReceiverAsIdentifier() != null ? aData.getReceiverAsIdentifier().getURIEncoded() : "";
        final String sDocTypeID = aData.getDocumentTypeAsIdentifier() != null ? aData.getDocumentTypeAsIdentifier().getURIEncoded() : "";
        final String sProcessID = aData.getProcessAsIdentifier() != null ? aData.getProcessAsIdentifier().getURIEncoded() : "";
        final String sCountryCodeC1 = aData.getCountryC1();
        LOGGER.info("Trying to send Peppol Test SBDH message from '" +
                sSenderID +
                "' to '" +
                sReceiverID +
                "' using '" +
                sDocTypeID +
                "' and '" +
                sProcessID +
                "' for '" +
                sCountryCodeC1 + "'");

        PeppolSender.sendPeppolMessagePredefinedSbdh(aData, eSML, aApCaChecker, aSendingReport);
        aSendingReport.setC2Cert(AS4CryptoFactoryConfiguration.getDefaultInstance().getCertificate());

        return ResponseEntity.ok(aSendingReport.getAsJsonString());
    }

    @PostMapping(path = "/canSend", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> canSendPeppolSbdhMessage(@RequestHeader(HEADER_X_TOKEN) final String xtoken,
                                           @RequestBody final byte[] aPayloadBytes) {
        if (StringHelper.isEmpty(xtoken)) {
            LOGGER.error("The specific token header is missing");
            throw new HttpForbiddenException();
        }
        if (!xtoken.equals(APConfig.getPhase4ApiRequiredToken())) {
            LOGGER.error("The specified token value does not match the configured required token");
            throw new HttpForbiddenException();
        }

        final EPeppolNetwork eStage = APConfig.getPeppolStage();
        final ESML eSML = eStage.isProduction() ? ESML.DIGIT_PRODUCTION : ESML.DIGIT_TEST;
        final LookupReport lookupReport = new LookupReport();

        final PeppolSBDHData aData;
        try {
            aData = new PeppolSBDHDataReader(PeppolIdentifierFactory.INSTANCE).extractData(new NonBlockingByteArrayInputStream(aPayloadBytes));
        } catch (final PeppolSBDHDataReadException ex) {
            lookupReport.setLookupCompleted(false);
            lookupReport.setReceiverExist(false);
            lookupReport.setErrorMessage(ex.toString());
            return ResponseEntity.badRequest().body(lookupReport.convertToJsonString());
        }

        try {
            boolean receiverExists = canReceiveDocument(
                aData.getReceiverAsIdentifier(),
                aData.getDocumentTypeAsIdentifier(),
                aData.getProcessAsIdentifier(),
                eSML
            );
            lookupReport.setLookupCompleted(true);
            lookupReport.setReceiverExist(receiverExists);
        } catch (Exception ex) {
            LOGGER.error("SMP/SML lookup failed", ex);
            lookupReport.setLookupCompleted(false);
            lookupReport.setReceiverExist(false);
            lookupReport.setErrorMessage(ex.toString());
        }

        return ResponseEntity.ok(lookupReport.convertToJsonString());
    }

    /**
     * Checks if a receiver can accept a document type and process via SMP/SML lookup.
     */
    public static boolean canReceiveDocument(
            com.helger.peppolid.IParticipantIdentifier receiver,
            com.helger.peppolid.IDocumentTypeIdentifier docType,
            com.helger.peppolid.IProcessIdentifier process,
            ESML sml) {
        try {
            // Create SMP client with the correct URL

            SMPClientReadOnly smpClient = new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                    receiver,
                    sml);

            // Check if ServiceGroup exists for the receiver
            var serviceGroup = smpClient.getServiceGroupOrNull(receiver);
            if (serviceGroup == null) {
                return false;
            }

            // Check if service metadata exists for document type
            var serviceMetadata = smpClient.getServiceMetadataOrNull(receiver, docType);
            if (serviceMetadata == null) {
                return false;
            }

            // Get the service information from the signed metadata
            var serviceInfo = serviceMetadata.getServiceMetadata().getServiceInformation();
            if (serviceInfo == null) {
                return false;
            }

            // Check if any process matches our target process
            var processList = serviceInfo.getProcessList();
            if (processList == null || processList.getProcessCount() == 0) {
                return false;
            }

            // Check if the specific process exists
            return processList.getProcess().stream()
                .anyMatch(p -> {
                    var pId = p.getProcessIdentifier();
                    return pId != null &&
                           process.getScheme().equals(pId.getScheme()) &&
                           process.getValue().equals(pId.getValue());
                });

        } catch (Exception e) {
            LOGGER.debug("SMP lookup failed for receiver: " + receiver, e);
            return false;
        }
    }

    /**
     * Validates the token. Returns null if valid, otherwise returns an error ResponseEntity.
     */
    private ResponseEntity<String> validateToken(final String xtoken)
    {
        if (StringHelper.isEmpty(xtoken))
        {
            return ResponseEntity
                    .badRequest()
                    .body("The specific token header is missing");
        }

        if (!xtoken.equals(APConfig.getPhase4ApiRequiredToken()))
        {
            return ResponseEntity
                    .badRequest()
                    .body("The specified token value does not match the configured required token");
        }

        return null; // Token is valid
    }
}
