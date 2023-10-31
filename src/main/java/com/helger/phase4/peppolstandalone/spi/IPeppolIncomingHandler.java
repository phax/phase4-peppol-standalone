/*
 * Copyright (C) 2023 Philip Helger (www.helger.com)
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

import org.unece.cefact.namespaces.sbdh.StandardBusinessDocument;

import com.helger.commons.http.HttpHeaderMap;
import com.helger.peppol.sbdh.PeppolSBDHDocument;
import com.helger.phase4.ebms3header.Ebms3UserMessage;
import com.helger.phase4.messaging.IAS4IncomingMessageMetadata;
import com.helger.phase4.servlet.IAS4MessageState;

public interface IPeppolIncomingHandler
{
  /**
   * Handle the provided incoming StandardBusinessDocument
   *
   * @param aMessageMetadata
   *        Message metadata. Includes data when and from whom it was
   *        received. Never <code>null</code>.
   * @param aHeaders
   *        The (HTTP) headers of the incoming request. Never
   *        <code>null</code>.
   * @param aUserMessage
   *        The received EBMS user message. Never <code>null</code>.
   * @param aSBDBytes
   *        The raw SBD bytes. Never <code>null</code>.
   * @param aSBD
   *        The incoming parsed Standard Business Document that is never
   *        <code>null</code>. This is the pre-parsed SBD bytes.
   * @param aPeppolSBD
   *        The pre-parsed Peppol Standard Business Document. Never
   *        <code>null</code>.
   * @param aState
   *        The message state. Can e.g. be used to retrieve information about
   *        the certificate found in the message. Never <code>null</code>.
   * @throws Exception
   *         In case it cannot be processed.
   */
  void handleIncomingSBD (@Nonnull IAS4IncomingMessageMetadata aMessageMetadata,
                          @Nonnull HttpHeaderMap aHeaders,
                          @Nonnull Ebms3UserMessage aUserMessage,
                          @Nonnull byte [] aSBDBytes,
                          @Nonnull StandardBusinessDocument aSBD,
                          @Nonnull PeppolSBDHDocument aPeppolSBD,
                          @Nonnull IAS4MessageState aState) throws Exception;
}