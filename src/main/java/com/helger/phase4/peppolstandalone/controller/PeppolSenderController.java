/*
 * Copyright (C) 2023-2025 Philip Helger (www.helger.com)
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
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReadException;
import com.helger.peppol.sbdh.read.PeppolSBDHDocumentReader;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.utils.PeppolCAChecker;
import com.helger.peppol.utils.PeppolCertificateChecker;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.client.IAS4ClientBuildMessageCallback;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.marshaller.Ebms3SignalMessageMarshaller;
import com.helger.phase4.model.message.AS4UserMessage;
import com.helger.phase4.model.message.AbstractAS4Message;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageSBDHBuilder;
import com.helger.phase4.peppolstandalone.APConfig;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xml.serialize.read.DOMReader;

/**
 * This is the primary REST controller for the APIs to send messages over
 * Peppol.
 *
 * @author Philip Helger
 */
@RestController
public class PeppolSenderController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolSenderController.class);

  @Nonnull
  private String _sendPeppolMessageCreatingSbdh (@Nonnull final byte [] aPayloadBytes,
                                                 @Nonnull final ISMLInfo aSmlInfo,
                                                 @Nonnull @Nonempty final String senderId,
                                                 @Nonnull @Nonempty final String receiverId,
                                                 @Nonnull @Nonempty final String docTypeId,
                                                 @Nonnull @Nonempty final String processId,
                                                 @Nonnull @Nonempty final String countryC1,
                                                 @Nonnull final PeppolCAChecker apCAChecker)
  {
    final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();

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
    boolean bExceptionCaught = false;
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      // Payload must be XML - even for Text and Binary content
      final Document aDoc = DOMReader.readXMLDOM (aPayloadBytes);
      if (aDoc == null)
        throw new IllegalStateException ("Failed to read provided payload as XML");

      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme (receiverId);

      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                  aReceiverID,
                                                                  aSmlInfo);

      aSMPClient.withHttpClientSettings (aHCS -> {
        // TODO Add SMP outbound proxy settings here
        // If this block is not used, it may be removed
      });

      if (EJavaVersion.getCurrentVersion ().isNewerOrEqualsThan (EJavaVersion.JDK_17))
      {
        // Work around the disabled SHA-1 in XMLDsig issue
        aSMPClient.setSecureValidation (false);
      }

      final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
      // TODO Add AP outbound proxy settings here

      final PeppolUserMessageBuilder aBuilder;
      aBuilder = Phase4PeppolSender.builder ()
                                   .httpClientFactory (aHCS)
                                   .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme (docTypeId))
                                   .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme (processId))
                                   .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme (senderId))
                                   .receiverParticipantID (aReceiverID)
                                   .senderPartyID (sMyPeppolSeatID)
                                   .countryC1 (countryC1)
                                   .payload (aDoc.getDocumentElement ())
                                   .peppolAP_CAChecker (apCAChecker)
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
                                       final IJsonArray aErrors = new JsonArray ();
                                       for (final Ebms3Error err : aSignalMsg.getError ())
                                       {
                                         final IJsonObject aErrorDetails = new JsonObject ();
                                         if (err.getDescription () != null)
                                           aErrorDetails.add ("description", err.getDescriptionValue ());
                                         if (err.getErrorDetail () != null)
                                           aErrorDetails.add ("errorDetails", err.getErrorDetail ());
                                         if (err.getCategory () != null)
                                           aErrorDetails.add ("category", err.getCategory ());
                                         if (err.getRefToMessageInError () != null)
                                           aErrorDetails.add ("refToMessageInError", err.getRefToMessageInError ());
                                         if (err.getErrorCode () != null)
                                           aErrorDetails.add ("errorCode", err.getErrorCode ());
                                         if (err.getOrigin () != null)
                                           aErrorDetails.add ("origin", err.getOrigin ());
                                         if (err.getSeverity () != null)
                                           aErrorDetails.add ("severity", err.getSeverity ());
                                         if (err.getShortDescription () != null)
                                           aErrorDetails.add ("shortDescription", err.getShortDescription ());
                                         aErrors.add (aErrorDetails);
                                         LOGGER.warn ("AS4 error received: " + aErrorDetails.getAsJsonString ());
                                       }
                                       aJson.add ("as4ResponseErrors", aErrors);
                                       aJson.add ("as4ResponseError", true);
                                     }
                                     else
                                       aJson.add ("as4ResponseError", false);
                                   })
                                   .disableValidation ();
      final Wrapper <Phase4Exception> aCaughtEx = new Wrapper <> ();
      eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtEx::set);
      LOGGER.info ("Peppol client send result: " + eResult);

      if (eResult.isSuccess ())
      {
        // TODO determine the enduser ID of the outbound message
        // In many simple cases, this might be the sender's participant ID
        final String sEndUserID = "TODO";

        // TODO Enable when ready
        if (false)
          aBuilder.createAndStorePeppolReportingItemAfterSending (sEndUserID);
      }

      aJson.add ("sendingResult", eResult);

      if (aCaughtEx.isSet ())
      {
        final Phase4Exception ex = aCaughtEx.get ();
        LOGGER.error ("Error sending Peppol message via AS4", ex);
        aJson.add ("sendingException",
                   new JsonObject ().add ("class", ex.getClass ().getName ())
                                    .add ("message", ex.getMessage ())
                                    .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
        bExceptionCaught = true;
      }
    }
    catch (final Exception ex)
    {
      // Mostly errors on HTTP level
      LOGGER.error ("Error sending Peppol message via AS4", ex);
      aJson.add ("sendingException",
                 new JsonObject ().add ("class", ex.getClass ().getName ())
                                  .add ("message", ex.getMessage ())
                                  .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
      bExceptionCaught = true;
    }
    finally
    {
      aSW.stop ();
      aJson.add ("overallDurationMillis", aSW.getMillis ());
    }

    // Result may be null
    final boolean bSendingSuccess = eResult != null && eResult.isSuccess ();
    aJson.add ("sendingSuccess", bSendingSuccess);
    aJson.add ("overallSuccess", bSendingSuccess && !bExceptionCaught);

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
    return _sendPeppolMessageCreatingSbdh (aPayloadBytes,
                                           ESML.DIGIT_TEST,
                                           senderId,
                                           receiverId,
                                           docTypeId,
                                           processId,
                                           countryC1,
                                           PeppolCertificateChecker.peppolTestAP ());
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
    return _sendPeppolMessageCreatingSbdh (aPayloadBytes,
                                           ESML.DIGIT_PRODUCTION,
                                           senderId,
                                           receiverId,
                                           docTypeId,
                                           processId,
                                           countryC1,
                                           PeppolCertificateChecker.peppolProductionAP ());
  }

  @Nonnull
  private String _sendPeppolMessagePredefinedSbdh (@Nonnull final PeppolSBDHData aData,
                                                   @Nonnull final ISMLInfo aSmlInfo,
                                                   @Nonnull final PeppolCAChecker apCAChecker)
  {
    final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();

    final OffsetDateTime aNowUTC = PDTFactory.getCurrentOffsetDateTimeUTC ();
    final IJsonObject aJson = new JsonObject ();
    aJson.add ("currentDateTimeUTC", PDTWebDateHelper.getAsStringXSD (aNowUTC));
    aJson.add ("senderId", aData.getSenderAsIdentifier ().getURIEncoded ());
    aJson.add ("receiverId", aData.getReceiverAsIdentifier ().getURIEncoded ());
    aJson.add ("docTypeId", aData.getDocumentTypeAsIdentifier ().getURIEncoded ());
    aJson.add ("processId", aData.getProcessAsIdentifier ().getURIEncoded ());
    aJson.add ("countryC1", aData.getCountryC1 ());
    aJson.add ("senderPartyId", sMyPeppolSeatID);

    EAS4UserMessageSendResult eResult = null;
    boolean bExceptionCaught = false;
    final StopWatch aSW = StopWatch.createdStarted ();
    try
    {
      // Start configuring here
      final IParticipantIdentifier aReceiverID = aData.getReceiverAsIdentifier ();

      final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                  aReceiverID,
                                                                  aSmlInfo);

      aSMPClient.withHttpClientSettings (aHCS -> {
        // TODO Add SMP outbound proxy settings here
        // If this block is not used, it may be removed
      });

      if (EJavaVersion.getCurrentVersion ().isNewerOrEqualsThan (EJavaVersion.JDK_17))
      {
        // Work around the disabled SHA-1 in XMLDsig issue
        aSMPClient.setSecureValidation (false);
      }

      final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
      // TODO Add AP outbound proxy settings here

      final PeppolUserMessageSBDHBuilder aBuilder;
      aBuilder = Phase4PeppolSender.sbdhBuilder ()
                                   .httpClientFactory (aHCS)
                                   .payloadAndMetadata (aData)
                                   .senderPartyID (sMyPeppolSeatID)
                                   .peppolAP_CAChecker (apCAChecker)
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
                                       aJson.add ("as4ResponseError", true);
                                       final IJsonArray aErrors = new JsonArray ();
                                       for (final Ebms3Error err : aSignalMsg.getError ())
                                       {
                                         final IJsonObject aErrorDetails = new JsonObject ();
                                         if (err.getDescription () != null)
                                           aErrorDetails.add ("description", err.getDescriptionValue ());
                                         if (err.getErrorDetail () != null)
                                           aErrorDetails.add ("errorDetails", err.getErrorDetail ());
                                         if (err.getCategory () != null)
                                           aErrorDetails.add ("category", err.getCategory ());
                                         if (err.getRefToMessageInError () != null)
                                           aErrorDetails.add ("refToMessageInError", err.getRefToMessageInError ());
                                         if (err.getErrorCode () != null)
                                           aErrorDetails.add ("errorCode", err.getErrorCode ());
                                         if (err.getOrigin () != null)
                                           aErrorDetails.add ("origin", err.getOrigin ());
                                         if (err.getSeverity () != null)
                                           aErrorDetails.add ("severity", err.getSeverity ());
                                         if (err.getShortDescription () != null)
                                           aErrorDetails.add ("shortDescription", err.getShortDescription ());
                                         aErrors.add (aErrorDetails);
                                         LOGGER.warn ("AS4 error received: " + aErrorDetails.getAsJsonString ());
                                       }
                                       aJson.add ("as4ResponseErrors", aErrors);
                                     }
                                     else
                                       aJson.add ("as4ResponseError", false);
                                   });
      final Wrapper <Phase4Exception> aCaughtEx = new Wrapper <> ();
      eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtEx::set);
      LOGGER.info ("Peppol client send result: " + eResult);

      if (eResult.isSuccess ())
      {
        // TODO determine the enduser ID of the outbound message
        // In many simple cases, this might be the sender's participant ID
        final String sEndUserID = "TODO";

        // TODO Enable when ready
        if (false)
          aBuilder.createAndStorePeppolReportingItemAfterSending (sEndUserID);
      }

      aJson.add ("sendingResult", eResult);

      if (aCaughtEx.isSet ())
      {
        final Phase4Exception ex = aCaughtEx.get ();
        LOGGER.error ("Error sending Peppol message via AS4", ex);
        aJson.add ("sendingException",
                   new JsonObject ().add ("class", ex.getClass ().getName ())
                                    .add ("message", ex.getMessage ())
                                    .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
        bExceptionCaught = true;
      }
    }
    catch (final Exception ex)
    {
      // Mostly errors on HTTP level
      LOGGER.error ("Error sending Peppol message via AS4", ex);
      aJson.add ("sendingException",
                 new JsonObject ().add ("class", ex.getClass ().getName ())
                                  .add ("message", ex.getMessage ())
                                  .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
      bExceptionCaught = true;
    }
    finally
    {
      aSW.stop ();
      aJson.add ("overallDurationMillis", aSW.getMillis ());
    }

    // Result may be null
    final boolean bSendingSuccess = eResult != null && eResult.isSuccess ();
    aJson.add ("sendingSuccess", bSendingSuccess);
    aJson.add ("overallSuccess", bSendingSuccess && !bExceptionCaught);

    // Return result JSON
    return aJson.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
  }

  @PostMapping (path = "/sendsbdhtest", produces = MediaType.APPLICATION_JSON_VALUE)
  public String sendPeppolTestMessage (@RequestBody final byte [] aPayloadBytes)
  {
    final PeppolSBDHData aData;
    try
    {
      aData = new PeppolSBDHDocumentReader (PeppolIdentifierFactory.INSTANCE).extractData (new NonBlockingByteArrayInputStream (aPayloadBytes));
    }
    catch (final PeppolSBDHDocumentReadException ex)
    {
      // TODO This error handling might be improved to return a status error
      // instead
      final IJsonObject aJson = new JsonObject ();
      aJson.add ("sbdhParsingException",
                 new JsonObject ().add ("class", ex.getClass ().getName ())
                                  .add ("message", ex.getMessage ())
                                  .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
      aJson.add ("success", false);
      return aJson.getAsJsonString (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);
    }

    final String senderId = aData.getSenderAsIdentifier ().getURIEncoded ();
    final String receiverId = aData.getReceiverAsIdentifier ().getURIEncoded ();
    final String docTypeId = aData.getDocumentTypeAsIdentifier ().getURIEncoded ();
    final String processId = aData.getProcessAsIdentifier ().getURIEncoded ();
    final String countryC1 = aData.getCountryC1 ();

    LOGGER.info ("Trying to send Peppol Test SBDH message from '" +
                 senderId +
                 "' to '" +
                 receiverId +
                 "' using '" +
                 docTypeId +
                 "' and '" +
                 processId +
                 "' for '" +
                 countryC1 +
                 "'");

    return _sendPeppolMessagePredefinedSbdh (aData, ESML.DIGIT_TEST, PeppolCertificateChecker.peppolTestAP ());
  }
}
