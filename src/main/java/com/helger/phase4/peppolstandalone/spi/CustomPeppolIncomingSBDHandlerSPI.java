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
package com.helger.phase4.peppolstandalone.spi;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.utils.PeppolCertificateHelper;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.ebms3header.Ebms3Error;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.incoming.IAS4IncomingMessageMetadata;
import com.helger.phase4.incoming.IAS4IncomingMessageState;
import com.helger.phase4.peppol.servlet.IPhase4PeppolIncomingSBDHandlerSPI;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;

/**
 * This is a way of handling incoming Peppol messages
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class CustomPeppolIncomingSBDHandlerSPI implements IPhase4PeppolIncomingSBDHandlerSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CustomPeppolIncomingSBDHandlerSPI.class);

  public void handleIncomingSBD (@Nonnull final IAS4IncomingMessageMetadata aMessageMetadata,
                                 @Nonnull final HttpHeaderMap aHeaders,
                                 @Nonnull final Ebms3UserMessage aUserMessage,
                                 @Nonnull final byte [] aSBDBytes,
                                 @Nonnull final StandardBusinessDocument aSBD,
                                 @Nonnull final PeppolSBDHData aPeppolSBD,
                                 @Nonnull final IAS4IncomingMessageState aState,
                                 @Nonnull final ICommonsList <Ebms3Error> aProcessingErrorMessages) throws Exception
  {
    // Example code snippets how to get data
    LOGGER.info ("Received a new Peppol Message");
    LOGGER.info ("  C1 = " + aPeppolSBD.getSenderAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  C2 = " + PeppolCertificateHelper.getSubjectCN (aState.getUsedCertificate ()));
    // C3 is you
    LOGGER.info ("  C4 = " + aPeppolSBD.getReceiverAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  DocType = " + aPeppolSBD.getDocumentTypeAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  Process = " + aPeppolSBD.getProcessAsIdentifier ().getURIEncoded ());
    LOGGER.info ("  CountryC1 = " + aPeppolSBD.getCountryC1 ());

    // TODO add your code here
    // E.g. write to disk, write to S3, write to database, write to queue...
    LOGGER.error ("You need to implement handleIncomingSBD to deal with incoming messages");

    // In case there is an error, send an Exception

    // Last action in this method
    new Thread ( () -> {
      // TODO If you have a way to determine the real end user of the message
      // here, this might be a good opportunity to store the data for Peppol
      // Reporting (do this asynchronously as the last activity)
      // Note: this is a separate thread so that it does not block the sending
      // of the positive receipt message

      // Peppol Reporting - enable if possible to be done in here
      if (false)
        try
        {
          LOGGER.info ("Creating Peppol Reporting Item and storing it");

          // TODO determine correct values
          final String sC3ID = "TODO-C3-ID";
          final String sC4CountryCode = "AT";
          final String sEndUserID = "EndUserID";
          final PeppolReportingItem aReportingItem = Phase4PeppolServletMessageProcessorSPI.createPeppolReportingItemForReceivedMessage (aUserMessage,
                                                                                                                                         aPeppolSBD,
                                                                                                                                         aState,
                                                                                                                                         sC3ID,
                                                                                                                                         sC4CountryCode,
                                                                                                                                         sEndUserID);
          PeppolReportingBackend.withBackendDo (AS4Configuration.getConfig (),
                                                aBackend -> aBackend.storeReportingItem (aReportingItem));
        }
        catch (final PeppolReportingBackendException ex)
        {
          LOGGER.error ("Failed to store Peppol Reporting Item", ex);
          // TODO improve error handling
        }
    }).start ();
  }
}
