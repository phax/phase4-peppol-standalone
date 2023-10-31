package com.helger.phase4.peppolstandalone.controller;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Element;

import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phase4.dump.AS4RawResponseConsumerWriteToFile;
import com.helger.phase4.peppol.Phase4PeppolSender;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder.ESimpleUserMessageSendResult;
import com.helger.smpclient.peppol.SMPClientReadOnly;

@RestController
public class PeppolSenderController
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolSenderController.class);

  // TODO
  private static final String C2_PEPPOL_SEAT_ID = "POP000000";

  private void _sendPeppolMessage (@Nonnull final Element aPayloadElement,
                                   @Nonnull final ISMLInfo aSmlInfo,
                                   @Nonnull @Nonempty final String senderId,
                                   @Nonnull @Nonempty final String receiverId,
                                   @Nonnull @Nonempty final String docTypeId,
                                   @Nonnull @Nonempty final String processId,
                                   @Nonnull @Nonempty final String countryC1)
  {
    try
    {
      // Start configuring here
      final IParticipantIdentifier aReceiverID = Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme (receiverId);
      final ESimpleUserMessageSendResult eResult;
      eResult = Phase4PeppolSender.builder ()
                                  .documentTypeID (Phase4PeppolSender.IF.createDocumentTypeIdentifierWithDefaultScheme (docTypeId))
                                  .processID (Phase4PeppolSender.IF.createProcessIdentifierWithDefaultScheme (processId))
                                  .senderParticipantID (Phase4PeppolSender.IF.createParticipantIdentifierWithDefaultScheme (senderId))
                                  .receiverParticipantID (aReceiverID)
                                  .senderPartyID (C2_PEPPOL_SEAT_ID)
                                  .countryC1 (countryC1)
                                  .payload (aPayloadElement)
                                  .smpClient (new SMPClientReadOnly (Phase4PeppolSender.URL_PROVIDER,
                                                                     aReceiverID,
                                                                     aSmlInfo))
                                  .rawResponseConsumer (new AS4RawResponseConsumerWriteToFile ())
                                  .disableValidation ()
                                  .sendMessageAndCheckForReceipt ();
      LOGGER.info ("Peppol client send result: " + eResult);
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Error sending Peppol message via AS4", ex);
    }
  }

  @PostMapping ("/sendprod/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}")
  public void sendPeppolProdMessage (@RequestBody final Element aPayloadElement,
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
    _sendPeppolMessage (aPayloadElement, ESML.DIGIT_PRODUCTION, senderId, receiverId, docTypeId, processId, countryC1);
  }

  @PostMapping ("/sendtest/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}")
  public void sendPeppolTestMessage (@RequestBody final Element aPayloadElement,
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
    _sendPeppolMessage (aPayloadElement, ESML.DIGIT_TEST, senderId, receiverId, docTypeId, processId, countryC1);
  }
}
