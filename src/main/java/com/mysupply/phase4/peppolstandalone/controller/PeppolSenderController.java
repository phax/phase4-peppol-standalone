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

import java.time.OffsetDateTime;

import javax.annotation.Nonnull;

import com.helger.commons.string.StringHelper;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.security.certificate.TrustedCAChecker;
import com.mysupply.phase4.peppolstandalone.APConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.system.EJavaVersion;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.wrapper.Wrapper;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageSBDHBuilder;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;

@RestController
public class PeppolSenderController {
    static final String HEADER_X_TOKEN = "X-Token";
    private static final Logger LOGGER = LoggerFactory.getLogger(PeppolSenderController.class);

    @PostMapping(path = "/send", produces = MediaType.APPLICATION_JSON_VALUE)
    public String sendPeppolSbdhMessage (@RequestHeader (name = HEADER_X_TOKEN, required = true) final String xtoken,
                                         @RequestBody final byte [] aPayloadBytes)
    {
        if (StringHelper.hasNoText (xtoken))
        {
            LOGGER.error ("The specific token header is missing");
            throw new HttpForbiddenException ();
        }
        if (!xtoken.equals (APConfig.getPhase4ApiRequiredToken ()))
        {
            LOGGER.error ("The specified token value does not match the configured required token");
            throw new HttpForbiddenException ();
        }

        final EPeppolNetwork eStage = APConfig.getPeppolStage ();
        final ESML eSML = eStage.isProduction () ? ESML.DIGIT_PRODUCTION : ESML.DIGIT_TEST;
        final TrustedCAChecker aAPCA = eStage.isProduction () ? PeppolTrustedCA.peppolProductionAP () : PeppolTrustedCA
                .peppolTestAP ();
        final Phase4PeppolSendingReport aSendingReport = new Phase4PeppolSendingReport (eSML);

        final PeppolSBDHData aData;
        try
        {
            aData = new PeppolSBDHDataReader(PeppolIdentifierFactory.INSTANCE).extractData (new NonBlockingByteArrayInputStream (aPayloadBytes));
        }
        catch (final PeppolSBDHDataReadException ex)
        {
            // TODO This error handling might be improved to return a status error
            // instead
            aSendingReport.setSBDHParseException (ex);
            aSendingReport.setSendingSuccess (false);
            aSendingReport.setOverallSuccess (false);
            return aSendingReport.getAsJsonString ();
        }

        aSendingReport.setSenderID (aData.getSenderAsIdentifier ());
        aSendingReport.setReceiverID (aData.getReceiverAsIdentifier ());
        aSendingReport.setDocTypeID (aData.getDocumentTypeAsIdentifier ());
        aSendingReport.setProcessID (aData.getProcessAsIdentifier ());
        aSendingReport.setCountryC1 (aData.getCountryC1 ());
        aSendingReport.setSBDHInstanceIdentifier (aData.getInstanceIdentifier ());

        final String sSenderID = aData.getSenderAsIdentifier ().getURIEncoded ();
        final String sReceiverID = aData.getReceiverAsIdentifier ().getURIEncoded ();
        final String sDocTypeID = aData.getDocumentTypeAsIdentifier ().getURIEncoded ();
        final String sProcessID = aData.getProcessAsIdentifier ().getURIEncoded ();
        final String sCountryCodeC1 = aData.getCountryC1 ();
        LOGGER.info ("Trying to send Peppol Test SBDH message from '" +
                sSenderID +
                "' to '" +
                sReceiverID +
                "' using '" +
                sDocTypeID +
                "' and '" +
                sProcessID +
                "' for '" +
                sCountryCodeC1 +
                "'");

        PeppolSender.sendPeppolMessagePredefinedSbdh (aData, eSML, aAPCA, aSendingReport);

        // Return result JSON
        return aSendingReport.getAsJsonString ();
    }
}
