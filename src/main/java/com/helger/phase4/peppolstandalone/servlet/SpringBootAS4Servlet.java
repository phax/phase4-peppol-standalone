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
package com.helger.phase4.peppolstandalone.servlet;

import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.http.EHttpMethod;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.incoming.AS4IncomingProfileSelectorConstant;
import com.helger.phase4.incoming.AS4RequestHandler;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.model.pmode.resolve.AS4DefaultPModeResolver;
import com.helger.phase4.peppol.servlet.Phase4PeppolDefaultReceiverConfiguration;
import com.helger.phase4.peppol.servlet.Phase4PeppolReceiverConfiguration;
import com.helger.phase4.peppol.servlet.Phase4PeppolServletMessageProcessorSPI;
import com.helger.phase4.servlet.AS4UnifiedResponse;
import com.helger.phase4.servlet.AS4XServletHandler;
import com.helger.phase4.servlet.IAS4ServletRequestHandlerCustomizer;
import com.helger.security.certificate.CertificateHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXServlet;

import jakarta.annotation.Nonnull;

public class SpringBootAS4Servlet extends AbstractXServlet
{
  public SpringBootAS4Servlet ()
  {
    // Multipart is handled specifically inside
    settings ().setMultipartEnabled (false);

    // The main XServlet handler to handle the inbound request
    final AS4XServletHandler hdl = new AS4XServletHandler ();
    hdl.setRequestHandlerCustomizer (new IAS4ServletRequestHandlerCustomizer ()
    {
      public void customizeBeforeHandling (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                           @Nonnull final AS4UnifiedResponse aUnifiedResponse,
                                           @Nonnull final AS4RequestHandler aRequestHandler)
      {
        final AS4CryptoFactoryInMemoryKeyStore aCryptoFactory = ServletConfig.getCryptoFactoryToUse ();

        // This method refers to the outer static method
        aRequestHandler.setCryptoFactory (aCryptoFactory);

        // Specific setters, dependent on a specific AS4 profile ID
        // This example code only uses the global one (if any)
        final String sAS4ProfileID = AS4ProfileSelector.getDefaultAS4ProfileID ();
        if (StringHelper.isNotEmpty (sAS4ProfileID))
        {
          aRequestHandler.setPModeResolver (new AS4DefaultPModeResolver (sAS4ProfileID));
          aRequestHandler.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (sAS4ProfileID));

          // TODO Example code to disable PMode validation
          // Delete the block if you don't need it
          if (false)
          {
            final boolean bValidateAgainstProfile = false;
            aRequestHandler.setIncomingProfileSelector (new AS4IncomingProfileSelectorConstant (sAS4ProfileID,
                                                                                                bValidateAgainstProfile));
          }
        }

        // TODO Example code for changing the Peppol receiver data based on the
        // source URL
        // Delete the block if you don't need it
        if (false)
        {
          final String sUrl = aRequestScope.getURLDecoded ();

          // The receiver check data you want to set
          final Phase4PeppolReceiverConfiguration aReceiverCheckData;
          if (sUrl != null && sUrl.startsWith ("https://ap-prod.example.org/as4"))
          {
            aReceiverCheckData = Phase4PeppolReceiverConfiguration.builder ()
                                                                  .receiverCheckEnabled (true)
                                                                  .serviceMetadataProvider (new SMPClientReadOnly (URLHelper.getAsURI ("http://smp-prod.example.org")))
                                                                  .as4EndpointUrl ("https://ap-prod.example.org/as4")
                                                                  .apCertificate (CertificateHelper.convertStringToCertficateOrNull ("....Public Prod AP Cert...."))
                                                                  .sbdhIdentifierFactoryPeppol ()
                                                                  .performSBDHValueChecks (Phase4PeppolDefaultReceiverConfiguration.isPerformSBDHValueChecks ())
                                                                  .checkSBDHForMandatoryCountryC1 (Phase4PeppolDefaultReceiverConfiguration.isCheckSBDHForMandatoryCountryC1 ())
                                                                  .checkSigningCertificateRevocation (Phase4PeppolDefaultReceiverConfiguration.isCheckSigningCertificateRevocation ())
                                                                  .build ();
          }
          else
          {
            aReceiverCheckData = Phase4PeppolReceiverConfiguration.builder ()
                                                                  .receiverCheckEnabled (true)
                                                                  .serviceMetadataProvider (new SMPClientReadOnly (URLHelper.getAsURI ("http://smp-test.example.org")))
                                                                  .as4EndpointUrl ("https://ap-test.example.org/as4")
                                                                  .apCertificate (CertificateHelper.convertStringToCertficateOrNull ("....Public Test AP Cert...."))
                                                                  .sbdhIdentifierFactoryPeppol ()
                                                                  .performSBDHValueChecks (Phase4PeppolDefaultReceiverConfiguration.isPerformSBDHValueChecks ())
                                                                  .checkSBDHForMandatoryCountryC1 (Phase4PeppolDefaultReceiverConfiguration.isCheckSBDHForMandatoryCountryC1 ())
                                                                  .checkSigningCertificateRevocation (Phase4PeppolDefaultReceiverConfiguration.isCheckSigningCertificateRevocation ())
                                                                  .build ();
          }

          // Find the right SPI handler
          aRequestHandler.getProcessorOfType (Phase4PeppolServletMessageProcessorSPI.class)
                         .setReceiverCheckData (aReceiverCheckData);
        }
      }

      public void customizeAfterHandling (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                          @Nonnull final AS4UnifiedResponse aUnifiedResponse,
                                          @Nonnull final AS4RequestHandler aRequestHandler)
      {
        // empty
      }
    });

    // HTTP POST only
    handlerRegistry ().registerHandler (EHttpMethod.POST, hdl);
  }
}
