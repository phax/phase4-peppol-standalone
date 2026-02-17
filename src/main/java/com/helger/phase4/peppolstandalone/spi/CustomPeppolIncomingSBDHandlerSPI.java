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
package com.helger.phase4.peppolstandalone.spi;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;
import org.w3c.dom.Element;

import com.helger.annotation.Nonempty;
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
import com.helger.phase4.messaging.EAS4MessageMode;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;
import com.helger.phase4.peppolstandalone.APConfig;
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

    // Example code snippets how to get data
    LOGGER.info ("Received a new Peppol Message");
    LOGGER.info ("  C1 = " + aPeppolSBD.getSenderAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  C2 = " + CertificateHelper.getSubjectCN (aIncomingState.getSigningCertificate ()));
    LOGGER.info ("  C3 = " + sMyPeppolSeatID);
    LOGGER.info ("  C4 = " + aPeppolSBD.getReceiverAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  DocType = " + aPeppolSBD.getDocumentTypeAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  Process = " + aPeppolSBD.getProcessAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  CountryC1 = " + aPeppolSBD.getCountryC1 ());

    // TODO add your code here
    // E.g. write to disk, write to S3, write to database, write to queue...
    LOGGER.error ("You need to implement handleIncomingSBD to deal with incoming messages");

    if (false)
    {
      // TODO example code on how to identify Factur-X payloads
      final Element aXMLPayload = aPeppolSBD.getBusinessMessageNoClone ();
      if (ObjectFactory._BinaryContent_QNAME.getLocalPart ().equals (aXMLPayload.getLocalName ()) &&
        ObjectFactory._BinaryContent_QNAME.getNamespaceURI ().equals (aXMLPayload.getNamespaceURI ()))
      {
        if ("urn:peppol:doctype:pdf+xml".equals (aPeppolSBD.getStandard ()) &&
          "0".equals (aPeppolSBD.getTypeVersion ()) &&
          "factur-x".equals (aPeppolSBD.getType ()))
        {
          // Handle as Factur-X
          final BinaryContentType aBinaryContent = new PeppolSBDHPayloadBinaryMarshaller ().read (aXMLPayload);
          final byte [] aPDFBytes = aBinaryContent.getValue ();
          // TODO do something with the PDF bytes
        }
      }
    }

    // In case there is an error, throw any Exception -> will lead to an AS4
    // Error Message to the sender

    // Last action in this method
    new Thread ( () -> {
      // TODO If you have a way to determine the real end user of the message
      // here, this might be a good opportunity to store the data for Peppol
      // Reporting (do this asynchronously as the last activity)
      // Note: this is a separate thread so that it does not block the sending
      // of the positive receipt message

      // TODO Peppol Reporting - enable if possible to be done in here
      if (false)
        try
        {
          LOGGER.info ("Creating Peppol Reporting Item and storing it");

          // TODO determine correct values for the next three fields
          final String sC3ID = sMyPeppolSeatID;
          final String sC4CountryCode = "AT";
          final String sEndUserID = aPeppolSBD.getReceiverAsIdentifier ().getURIEncoded ();

          // Create the reporting item
          final PeppolReportingItem aReportingItem = Phase4PeppolServletMessageProcessorSPI.createPeppolReportingItemForReceivedMessage (aUserMessage,
                                                                                                                                         aPeppolSBD,
                                                                                                                                         aIncomingState,
                                                                                                                                         sC3ID,
                                                                                                                                         sC4CountryCode,
                                                                                                                                         sEndUserID);
          PeppolReportingBackend.withBackendDo (APConfig.getConfig (),
                                                aBackend -> aBackend.storeReportingItem (aReportingItem));
        }
        catch (final PeppolReportingBackendException ex)
        {
          LOGGER.error ("Failed to store Peppol Reporting Item", ex);
          // TODO improve error handling
        }
    }).start ();
  }

  public void processAS4ResponseMessage (@NonNull final IAS4IncomingMessageMetadata aIncomingMessageMetadata,
                                         @NonNull final IAS4IncomingMessageState aIncomingState,
                                         @NonNull @Nonempty final String sResponseMessageID,
                                         final byte @Nullable [] aResponseBytes,
                                         final boolean bResponsePayloadIsAvailable,
                                         @NonNull final AS4ErrorList aEbmsErrorMessages)
  {
    if (aIncomingMessageMetadata.getMode () == EAS4MessageMode.REQUEST)
      LOGGER.info ("AS4 response on an inbound message");
    else
      LOGGER.info ("AS4 response on an outbound message");

    if (bResponsePayloadIsAvailable)
      LOGGER.info ("  Response content: " + new String (aResponseBytes, StandardCharsets.UTF_8));
  }
}
