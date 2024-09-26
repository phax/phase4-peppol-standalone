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
package com.helger.phase4.peppolstandalone.controller;

import java.time.OffsetDateTime;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTWebDateHelper;
import com.helger.commons.lang.StackTraceHelper;
import com.helger.commons.system.EJavaVersion;
import com.helger.commons.timing.StopWatch;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xml.serialize.read.DOMReader;

@RestController
public class PeppolSenderController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolSenderController.class);

  @Nonnull
  private String _sendPeppolMessage (@Nonnull final byte [] aPayloadBytes,
                                     @Nonnull final ISMLInfo aSmlInfo,
                                     @Nonnull @Nonempty final String senderId,
                                     @Nonnull @Nonempty final String receiverId,
                                     @Nonnull @Nonempty final String docTypeId,
                                     @Nonnull @Nonempty final String processId,
                                     @Nonnull @Nonempty final String countryC1)
  {
    final String sMyPeppolSeatID = AS4Configuration.getConfig ().getAsString ("peppol.seatid");

    final OffsetDateTime aNowUTC = PDTFactory.getCurrentOffsetDateTimeUTC ();
    final IJsonObject aJson = new JsonObject ();
    aJson.add ("currentDateTimeUTC", PDTWebDateHelper.getAsStringXSD (aNowUTC));
    aJson.add ("senderId", senderId);
    aJson.add ("receiverId", receiverId);
    aJson.add ("docTypeId", docTypeId);
    aJson.add ("processId", processId);
    aJson.add ("countryC1", countryC1);
    aJson.add ("senderPartyId", sMyPeppolSeatID);

    EAS4UserMessageSendResult eResult = null;
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      final Document aDoc = DOMReader.readXMLDOM (aPayloadBytes);
      if (aDoc == null)
        throw new IllegalStateException ("Failed to read provided payload as XML");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme (receiverId);

      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                  aReceiverID,
                                                                  aSmlInfo);
      if (EJavaVersion.getCurrentVersion ().isNewerOrEqualsThan (EJavaVersion.JDK_17))
      {
        // Work around the disabled SHA-1 in XMLDsig issue
        aSMPClient.setSecureValidation (false);
      }

      final PeppolUserMessageBuilder aBuilder;
      aBuilder = Phase4PeppolSender.builder ()
                                   .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme (docTypeId))
                                   .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme (processId))
                                   .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme (senderId))
                                   .receiverParticipantID (aReceiverID)
                                   .senderPartyID (sMyPeppolSeatID)
                                   .countryC1 (countryC1)
                                   .payload (aDoc.getDocumentElement ())
                                   .smpClient (aSMPClient)
                                   .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                   .endpointURLConsumer (endpointUrl -> {
                                     // Determined by SMP lookup
                                     aJson.add ("c3EndpointUrl", endpointUrl);
                                   })
                                   .certificateConsumer ( (aAPCertificate, aCheckDT, eCertCheckResult) -> {
                                     // Determined by SMP lookup
                                     aJson.add ("c3Cert", CertificateHelper.getPEMEncodedCertificate (aAPCertificate));
                                     aJson.add ("c3CertSubjectCN",
                                                PeppolCertificateHelper.getSubjectCN (aAPCertificate));
                                     aJson.add ("c3CertCheckDT", PDTWebDateHelper.getAsStringXSD (aCheckDT));
                                     aJson.add ("c3CertCheckResult", eCertCheckResult);
                                   })
                                   .buildMessageCallback (new IAS4ClientBuildMessageCallback ()
                                   {
                                     public void onAS4Message (@Nonnull final AbstractAS4Message <?> aMsg)
                                     {
                                       // Created AS4 fields
                                       final AS4UserMessage aUserMsg = (AS4UserMessage) aMsg;
                                       aJson.add ("as4MessageId",
                                                  aUserMsg.getEbms3UserMessage ().getMessageInfo ().getMessageId ());
                                       aJson.add ("as4ConversationId",
                                                  aUserMsg.getEbms3UserMessage ()
                                                          .getCollaborationInfo ()
                                                          .getConversationId ());
                                     }
                                   })
                                   .signalMsgConsumer ( (aSignalMsg, aMessageMetadata, aState) -> {
                                     aJson.add ("as4ReceivedSignalMsg",
                                                new Ebms3SignalMessageMarshaller ().getAsString (aSignalMsg));

                                     if (aSignalMsg.hasErrorEntries ())
                                     {
                                       // TODO extract the errors
                                       aJson.add ("as4ResponseError", true);
                                     }
                                     else
                                       aJson.add ("as4ResponseError", false);
                                   })
                                   .disableValidation ();
      eResult = aBuilder.sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol client send result: " + eResult);

      if (eResult.isSuccess ())
      {
        // TODO determine the enduser ID of the outbound message
        final String sEndUserID = "TODO";

        // TODO Enable when ready
        if (false)
          aBuilder.createAndStorePeppolReportingItemAfterSending (sEndUserID);
      }

      aJson.add ("sendingResult", eResult);
    }
    catch (final Exception ex)
    {
      // Mostly errors on HTTP level
      LOGGER.error ("Error sending Peppol message via AS4", ex);
      aJson.add ("sendingException",
                 new JsonObject ().add ("class", ex.getClass ().getName ())
                                  .add ("message", ex.getMessage ())
                                  .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
    }
    finally
    {
      aSW.stop ();
      aJson.add ("overallDurationMillis", aSW.getMillis ());
    }

    // Result may be null
    aJson.add ("success", eResult == EAS4UserMessageSendResult.SUCCESS);

    // Return result JSON
    return aJson.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
  }

  @PostMapping (path = "/sendtest/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}",
                produces = MediaType.APPLICATION_JSON_VALUE)
  public String sendPeppolTestMessage (@RequestBody final byte [] aPayloadBytes,
                                       @PathVariable final String senderId,
                                       @PathVariable final String receiverId,
                                       @PathVariable final String docTypeId,
                                       @PathVariable final String processId,
                                       @PathVariable final String countryC1)
  {
    LOGGER.info ("Trying to send Peppol Test message from '" +
                 senderId +
                 "' to '" +
                 receiverId +
                 "' using '" +
                 docTypeId +
                 "' and '" +
                 processId +
                 "'");
    return _sendPeppolMessage (aPayloadBytes, ESML.DIGIT_TEST, senderId, receiverId, docTypeId, processId, countryC1);
  }

  @PostMapping (path = "/sendprod/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}",
                produces = MediaType.APPLICATION_JSON_VALUE)
  public String sendPeppolProdMessage (@RequestBody final byte [] aPayloadBytes,
                                       @PathVariable final String senderId,
                                       @PathVariable final String receiverId,
                                       @PathVariable final String docTypeId,
                                       @PathVariable final String processId,
                                       @PathVariable final String countryC1)
  {
    LOGGER.info ("Trying to send Peppol Prod message from '" +
                 senderId +
                 "' to '" +
                 receiverId +
                 "' using '" +
                 docTypeId +
                 "' and '" +
                 processId +
                 "'");
    return _sendPeppolMessage (aPayloadBytes,
                               ESML.DIGIT_PRODUCTION,
                               senderId,
                               receiverId,
                               docTypeId,
                               processId,
                               countryC1);
  }
}
