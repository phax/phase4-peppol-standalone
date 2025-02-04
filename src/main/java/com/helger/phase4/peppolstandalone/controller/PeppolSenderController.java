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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.helger.commons.io.stream.NonBlockingByteArrayInputStream;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sbdh.PeppolSBDHData;
import com.helger.peppol.sbdh.PeppolSBDHDataReadException;
import com.helger.peppol.sbdh.PeppolSBDHDataReader;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.utils.PeppolCertificateChecker;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phase4.peppolstandalone.APConfig;

/**
 * This is the primary REST controller for the APIs to send messages over
 * Peppol.
 *
 * @author Philip Helger
 */
@RestController
public class PeppolSenderController
{
  @ResponseStatus (HttpStatus.FORBIDDEN)
  public static class ForbiddenException extends RuntimeException
  {}

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolSenderController.class);
  private static final String HEADER_X_TOKEN = "X-Token";

  @PostMapping (path = "/sendtest/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String sendPeppolTestMessage (@RequestHeader (name = HEADER_X_TOKEN, required = true) final String xtoken,
                                       @RequestBody final byte [] aPayloadBytes,
                                       @PathVariable final String senderId,
                                       @PathVariable final String receiverId,
                                       @PathVariable final String docTypeId,
                                       @PathVariable final String processId,
                                       @PathVariable final String countryC1)
  {
    if (StringHelper.hasNoText (xtoken))
    {
      LOGGER.error ("The specific token header is missing");
      throw new ForbiddenException ();
    }
    if (!xtoken.equals (APConfig.getPhase4ApiRequiredToken ()))
    {
      LOGGER.error ("The specified token value does not match the configured required token");
      throw new ForbiddenException ();
    }

    LOGGER.info ("Trying to send Peppol Test message from '" +
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
    final PeppolSendingReport aSendingReport = PeppolSender.sendPeppolMessageCreatingSbdh (ESML.DIGIT_TEST,
                                                                                           PeppolCertificateChecker.peppolTestAP (),
                                                                                           aPayloadBytes,
                                                                                           senderId,
                                                                                           receiverId,
                                                                                           docTypeId,
                                                                                           processId,
                                                                                           countryC1);

    // Return as JSON
    return aSendingReport.getAsJsonString ();
  }

  @PostMapping (path = "/sendprod/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}", produces = MediaType.APPLICATION_JSON_VALUE)
  public String sendPeppolProdMessage (@RequestHeader (name = HEADER_X_TOKEN, required = true) final String xtoken,
                                       @RequestBody final byte [] aPayloadBytes,
                                       @PathVariable final String senderId,
                                       @PathVariable final String receiverId,
                                       @PathVariable final String docTypeId,
                                       @PathVariable final String processId,
                                       @PathVariable final String countryC1)
  {
    if (StringHelper.hasNoText (xtoken))
    {
      LOGGER.error ("The specific token header is missing");
      throw new ForbiddenException ();
    }
    if (!xtoken.equals (APConfig.getPhase4ApiRequiredToken ()))
    {
      LOGGER.error ("The specified token value does not match the configured required token");
      throw new ForbiddenException ();
    }

    LOGGER.info ("Trying to send Peppol Prod message from '" +
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
    final PeppolSendingReport aSendingReport = PeppolSender.sendPeppolMessageCreatingSbdh (ESML.DIGIT_PRODUCTION,
                                                                                           PeppolCertificateChecker.peppolProductionAP (),
                                                                                           aPayloadBytes,
                                                                                           senderId,
                                                                                           receiverId,
                                                                                           docTypeId,
                                                                                           processId,
                                                                                           countryC1);

    // Return as JSON
    return aSendingReport.getAsJsonString ();
  }

  @PostMapping (path = "/sendsbdhtest", produces = MediaType.APPLICATION_JSON_VALUE)
  public String sendPeppolTestSbdhMessage (@RequestHeader (name = HEADER_X_TOKEN, required = true) final String xtoken,
                                           @RequestBody final byte [] aPayloadBytes)
  {
    if (StringHelper.hasNoText (xtoken))
    {
      LOGGER.error ("The specific token header is missing");
      throw new ForbiddenException ();
    }
    if (!xtoken.equals (APConfig.getPhase4ApiRequiredToken ()))
    {
      LOGGER.error ("The specified token value does not match the configured required token");
      throw new ForbiddenException ();
    }

    final ESML eSML = ESML.DIGIT_TEST;
    final PeppolSendingReport aSendingReport = new PeppolSendingReport (eSML);

    final PeppolSBDHData aData;
    try
    {
      aData = new PeppolSBDHDataReader (PeppolIdentifierFactory.INSTANCE).extractData (new NonBlockingByteArrayInputStream (aPayloadBytes));
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

    PeppolSender.sendPeppolMessagePredefinedSbdh (aData,
                                                  eSML,
                                                  PeppolCertificateChecker.peppolTestAP (),
                                                  aSendingReport);

    // Return result JSON
    return aSendingReport.getAsJsonString ();
  }
}
