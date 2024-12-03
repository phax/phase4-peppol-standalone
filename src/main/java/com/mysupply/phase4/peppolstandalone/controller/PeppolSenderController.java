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
import javax.naming.ConfigurationException;

import com.helger.commons.exception.InitializationException;
import com.mysupply.phase4.domain.enums.MetadataProviderEnum;
import jakarta.servlet.http.HttpServletResponse;
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
import com.helger.peppol.utils.PeppolCertificateHelper;
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
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageBuilder;
import com.helger.phase4.peppol.Phase4PeppolSender.PeppolUserMessageSBDHBuilder;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.phase4.sender.EAS4UserMessageSendResult;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.xml.serialize.read.DOMReader;

@RestController
public class PeppolSenderController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolSenderController.class);

  @Nonnull
  private String _sendPeppolMessagePredefinedSbdh (@Nonnull final PeppolSBDHData aData,
                                                   @Nonnull final ISMLInfo aSmlInfo,
                                                   @Nonnull HttpServletResponse aHttpResponse)
  {
    final String sMyPeppolSeatID = AS4Configuration.getConfig ().getAsString ("peppol.seatid");

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
                                       }
                                       aJson.add ("as4ResponseErrors", aErrors);
                                     }
                                     else
                                       aJson.add ("as4ResponseError", false);
                                   });
      final Wrapper <Phase4Exception> aCaughtEx = new Wrapper <> ();
      eResult = aBuilder.sendMessageAndCheckForReceipt (aCaughtEx::set);
      LOGGER.info ("Peppol client send result: " + eResult);

//      if (eResult.isSuccess ())
//      {
//        // TODO determine the enduser ID of the outbound message
//        // In many simple cases, this might be the sender's participant ID
//        final String sEndUserID = "TODO";
//
//        // TODO Enable when ready
//        if (false)
//          aBuilder.createAndStorePeppolReportingItemAfterSending (sEndUserID);
//      }

      aJson.add ("sendingResult", eResult);

      if (aCaughtEx.isSet ())
      {
        final Phase4Exception ex = aCaughtEx.get ();
        LOGGER.error ("Error sending Peppol message via AS4", ex);
        aJson.add ("sendingException",
                   new JsonObject ().add ("class", ex.getClass ().getName ())
                                    .add ("message", ex.getMessage ())
                                    .add ("stackTrace", StackTraceHelper.getStackAsString (ex)));
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

  @PostMapping (path = "/send", produces = MediaType.APPLICATION_JSON_VALUE)
  public String sendMessage (@RequestBody final byte [] aPayloadBytes, HttpServletResponse aHttpResponse) throws ConfigurationException {
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
                                  .add ("message", ex.getMessage ()));
      LOGGER.error("An error occurred during receival of outgoing message.", ex);
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

    String smlToUse = AS4Configuration.getConfig ().getAsString("peppol.smlToUse");
    if(smlToUse == null || smlToUse.isEmpty()) {
      throw new ConfigurationException("peppol.smlToUse is not set in the configuration.");
    } else if(smlToUse.equalsIgnoreCase(MetadataProviderEnum.SML.name())) {
      return _sendPeppolMessagePredefinedSbdh (aData, ESML.DIGIT_PRODUCTION, aHttpResponse);
    } else if(smlToUse.equalsIgnoreCase(MetadataProviderEnum.SMK.name())) {
      return _sendPeppolMessagePredefinedSbdh (aData, ESML.DIGIT_TEST, aHttpResponse);
    } else {
      throw new ConfigurationException("peppol.smlToUse is not set to a valid value in the configuration.");
    }
  }
}
