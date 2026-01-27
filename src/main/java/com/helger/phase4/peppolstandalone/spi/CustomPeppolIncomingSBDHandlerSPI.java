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
package com.helger.phase4.peppolstandalone.spi;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Element;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.http.header.HttpHeaderMap;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.payload.PeppolSBDHPayloadBinaryMarshaller;
import com.helger.peppol.sbdh.spec12.BinaryContentType;
import com.helger.peppol.sbdh.spec12.ObjectFactory;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.error.AS4ErrorList;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;
import com.helger.phase4.peppolstandalone.APConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helger.xml.serialize.write.XMLWriter;
import com.helger.phase4.util.Phase4Exception;
import com.helger.security.certificate.CertificateHelper;

/**
 * This is a way of handling incoming Peppol messages
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class CustomPeppolIncomingSBDHandlerSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (CustomPeppolIncomingSBDHandlerSPI.class);

  public void handleIncomingSBD (@NonNull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @NonNull final HttpHeaderMap aHeaders,
                                 @NonNull final Ebms3UserMessage aUserMessage,
                                 @NonNull final byte [] aSBDBytes,
                                 @NonNull final StandardBusinessDocument aSBD,
                                 @NonNull final PeppolSBDHData aPeppolSBD,
                                 @NonNull final IAS4IncomingMessageState aIncomingState,
                                 @NonNull final AS4ErrorList aProcessingErrorMessages) throws Exception
  {
    if (!APConfig.isReceivingEnabled ())
    {
      LOGGER.info ("Peppol AP receiving is disabled");
      throw new Phase4Exception ("Peppol AP receiving is disabled");
    }

    final String sMyPeppolSeatID = APConfig.getMyPeppolSeatID ();

    String senderId = aPeppolSBD.getSenderAsIdentifier().getURIEncoded();
    String receiverId = aPeppolSBD.getReceiverAsIdentifier().getURIEncoded();
    String docTypeId = aPeppolSBD.getDocumentTypeAsIdentifier().getURIEncoded();
    String processId = aPeppolSBD.getProcessAsIdentifier().getURIEncoded();
    String countryC1 = aPeppolSBD.getCountryC1();
    String body = XMLWriter.getNodeAsString(aPeppolSBD.getBusinessMessage());
    String instanceIdentifier = aPeppolSBD.getInstanceIdentifier();
    
    // Extract AS4 message metadata
    String as4MessageId = aUserMessage.getMessageInfo() != null && aUserMessage.getMessageInfo().getMessageId() != null 
        ? aUserMessage.getMessageInfo().getMessageId() 
        : null;
    String as4ConversationId = aUserMessage.getCollaborationInfo() != null && aUserMessage.getCollaborationInfo().getConversationId() != null 
        ? aUserMessage.getCollaborationInfo().getConversationId() 
        : null;

    // Create JSON payload using Jackson
    Map<String, Object> payloadMap = new HashMap<>();
    payloadMap.put("senderId", senderId);
    payloadMap.put("receiverId", receiverId);
    payloadMap.put("docTypeId", docTypeId);
    payloadMap.put("processId", processId);
    payloadMap.put("countryC1", countryC1);
    payloadMap.put("body", body);
    payloadMap.put("as4MessageId", as4MessageId);
    payloadMap.put("as4ConversationId", as4ConversationId);
    payloadMap.put("sbdhInstanceIdentifier", instanceIdentifier);

    String jsonPayload = new ObjectMapper().writeValueAsString(payloadMap);

    LOGGER.info("Received document from " + senderId + " to " + receiverId + " with docTypeId " + docTypeId + " and processId " + processId + " and countryC1 " + countryC1);
    LOGGER.info("About to send document to: " + APConfig.getRecommandApiEndpoint() + "/api/peppol/internal/receiveDocument");

    // Send to endpoint
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(APConfig.getRecommandApiEndpoint() + "/api/peppol/internal/receiveDocument"))
          .header("Content-Type", "application/json")
          .header("X-Internal-Token", APConfig.getRecommandApiInternalToken())
          .POST(BodyPublishers.ofString(jsonPayload))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      
      if (response.statusCode() != 200) {
        LOGGER.error("Failed to send document to endpoint. Status code: " + response.statusCode());
        throw new Exception("Failed to process document");
      }
      
      LOGGER.info("Successfully sent document to endpoint");
    } catch (Exception e) {
      // In case there is an error, throw any Exception -> will lead to an AS4
      // Error Message to the sender
      LOGGER.error("Error sending document to endpoint", e);
      throw e;
    }

    // Last action in this method
    new Thread ( () -> {
      // TODO If you have a way to determine the real end user of the message
      // here, this might be a good opportunity to store the data for Peppol
      // Reporting (do this asynchronously as the last activity)
      // Note: this is a separate thread so that it does not block the sending
      // of the positive receipt message

      // Peppol Reporting - enabled for incoming messages
      try
      {
        LOGGER.info ("Creating Peppol Reporting Item and storing it");

        // Determine correct values for the required fields
        final String sC3ID = sMyPeppolSeatID;
        final String sC4CountryCode = APConfig.getMyPeppolCountryCode();
        
        // Determine end user ID - in most cases, this would be the receiver's participant ID
        // since this is an incoming message handler, the receiver is the end user
        final String sEndUserID = aPeppolSBD.getReceiverAsIdentifier().getURIEncoded();

        // Create the reporting item
        final PeppolReportingItem aReportingItem = Phase4PeppolServletMessageProcessorSPI.createPeppolReportingItemForReceivedMessage (aUserMessage,
                                                                                                                                       aPeppolSBD,
                                                                                                                                       aIncomingState,
                                                                                                                                       sC3ID,
                                                                                                                                       sC4CountryCode,
                                                                                                                                       sEndUserID);
        PeppolReportingBackend.withBackendDo (APConfig.getConfig (),
                                              aBackend -> aBackend.storeReportingItem (aReportingItem));
        
        LOGGER.info ("Successfully stored Peppol Reporting Item for end user: " + sEndUserID);
      }
      catch (final PeppolReportingBackendException ex)
      {
        LOGGER.error ("Failed to store Peppol Reporting Item", ex);
        // Don't throw the exception as this is in a separate thread and shouldn't affect the main message processing
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Unexpected error during Peppol Reporting", ex);
        // Don't throw the exception as this is in a separate thread and shouldn't affect the main message processing
      }
    }).start ();
  }
}
