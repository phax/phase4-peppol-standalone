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

import java.io.File;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import com.helger.base.debug.GlobalDebug;
import com.helger.base.exception.InitializationException;
import com.helger.base.state.ETriState;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.httpclient.HttpDebugger;
import com.helger.mime.CMimeType;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.security.PeppolTrustedCA;
import com.helger.peppol.servicedomain.EPeppolNetwork;
import com.helger.phase4.config.AS4Configuration;
import com.helger.phase4.crypto.AS4CryptoFactoryConfiguration;
import com.helger.phase4.crypto.AS4CryptoFactoryInMemoryKeyStore;
import com.helger.phase4.crypto.IAS4CryptoFactory;
import com.helger.phase4.dump.AS4DumpManager;
import com.helger.phase4.dump.AS4IncomingDumperFileBased;
import com.helger.phase4.dump.AS4OutgoingDumperFileBased;
import com.helger.phase4.incoming.AS4ServerInitializer;
import com.helger.phase4.incoming.mgr.AS4ProfileSelector;
import com.helger.phase4.logging.Phase4LoggerFactory;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.peppol.servlet.Phase4PeppolDefaultReceiverConfiguration;
import com.helger.phase4.peppolstandalone.APConfig;
import com.helger.phase4.peppolstandalone.reporting.AppReportingHelper;
import com.helger.phase4.profile.peppol.AS4PeppolProfileRegistarSPI;
import com.helger.phase4.profile.peppol.PeppolCRLDownloader;
import com.helger.phase4.profile.peppol.Phase4PeppolHttpClientSettings;
import com.helger.photon.io.WebFileIO;
import com.helger.security.certificate.ECertificateCheckResult;
import com.helger.security.certificate.TrustedCAChecker;
import com.helger.servlet.ServletHelper;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.mgr.WebScopeManager;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.activation.CommandMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletContext;

@Configuration
public class ServletConfig
{
  private static final Logger LOGGER = Phase4LoggerFactory.getLogger (ServletConfig.class);

  /**
   * This method is a placeholder for retrieving a custom {@link IAS4CryptoFactory}.
   *
   * @return the {@link IAS4CryptoFactory} to use. May not be <code>null</code>.
   */
  @Nonnull
  public static AS4CryptoFactoryInMemoryKeyStore getCryptoFactoryToUse ()
  {
    final AS4CryptoFactoryConfiguration ret = AS4CryptoFactoryConfiguration.getDefaultInstance ();
    // TODO If you have a custom crypto factory, build/return it here
    return ret;
  }

  @Bean
  public ServletRegistrationBean <SpringBootAS4Servlet> servletRegistrationBean (final ServletContext ctx)
  {
    // Must be called BEFORE the servlet is instantiated
    _init (ctx);

    // Instantiate and register Servlet
    final ServletRegistrationBean <SpringBootAS4Servlet> bean = new ServletRegistrationBean <> (new SpringBootAS4Servlet (),
                                                                                                true,
                                                                                                "/as4");
    bean.setLoadOnStartup (1);
    return bean;
  }

  private void _init (@Nonnull final ServletContext aSC)
  {
    // Do it only once
    if (!WebScopeManager.isGlobalScopePresent ())
    {
      WebScopeManager.onGlobalBegin (aSC);
      _initGlobalSettings (aSC);
      _initAS4 ();
      _initPeppolAS4 ();
    }
  }

  private static void _initGlobalSettings (@Nonnull final ServletContext aSC)
  {
    // Logging: JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    if (GlobalDebug.isDebugMode ())
    {
      RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
      RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    }

    HttpDebugger.setEnabled (false);

    // Sanity check
    if (CommandMap.getDefaultCommandMap ().createDataContentHandler (CMimeType.MULTIPART_RELATED.getAsString ()) ==
        null)
    {
      throw new IllegalStateException ("No DataContentHandler for MIME Type '" +
                                       CMimeType.MULTIPART_RELATED.getAsString () +
                                       "' is available. There seems to be a problem with the dependencies/packaging");
    }

    // Init the data path
    {
      // Get the ServletContext base path
      final String sServletContextPath = ServletHelper.getServletContextBasePath (aSC);
      // Get the data path
      final String sDataPath = AS4Configuration.getDataPath ();
      if (StringHelper.isEmpty (sDataPath))
        throw new InitializationException ("No data path was provided!");
      final boolean bFileAccessCheck = false;
      // Init the IO layer
      WebFileIO.initPaths (new File (sDataPath).getAbsoluteFile (), sServletContextPath, bFileAccessCheck);
    }
  }

  private static void _initAS4 ()
  {
    // Enforce Peppol profile usage
    // This is the programmatic way to enforce exactly this one profile
    // In a multi-profile environment, that will not work
    AS4ProfileSelector.setCustomDefaultAS4ProfileID (AS4PeppolProfileRegistarSPI.AS4_PROFILE_ID);

    AS4ServerInitializer.initAS4Server ();

    // dump all messages to a file
    AS4DumpManager.setIncomingDumper (new AS4IncomingDumperFileBased ());
    AS4DumpManager.setOutgoingDumper (new AS4OutgoingDumperFileBased ());
  }

  private static void _initPeppolAS4 ()
  {
    // Make sure the download of CRL is using Apache HttpClient and that the
    // provided settings are used. If e.g. a proxy is needed to access outbound
    // resources, it can be configured here
    {
      final Phase4PeppolHttpClientSettings aHCS = new Phase4PeppolHttpClientSettings ();
      // TODO eventually configure an outbound HTTP proxy here as well
      PeppolCRLDownloader.setAsDefaultCRLCache (aHCS);
    }

    // Throws an exception if configuration parameters are missing
    final AS4CryptoFactoryInMemoryKeyStore aCryptoFactory = getCryptoFactoryToUse ();

    // Check if crypto factory configuration is valid
    final KeyStore aKS = aCryptoFactory.getKeyStore ();
    if (aKS == null)
      throw new InitializationException ("Failed to load configured AS4 Key store - fix the configuration");
    LOGGER.info ("Successfully loaded configured AS4 key store from the crypto factory");

    final KeyStore.PrivateKeyEntry aPKE = aCryptoFactory.getPrivateKeyEntry ();
    if (aPKE == null)
      throw new InitializationException ("Failed to load configured AS4 private key - fix the configuration");
    LOGGER.info ("Successfully loaded configured AS4 private key from the crypto factory");

    // Configure the stage correctly
    final EPeppolNetwork eStage = APConfig.getPeppolStage ();

    final X509Certificate aAPCert = (X509Certificate) aPKE.getCertificate ();

    final TrustedCAChecker aAPCAChecker = eStage.isProduction () ? PeppolTrustedCA.peppolProductionAP ()
                                                                 : PeppolTrustedCA.peppolTestAP ();

    // Check the configured Peppol AP certificate
    // * No caching
    // * Use global certificate check mode
    final ECertificateCheckResult eCheckResult = aAPCAChecker.checkCertificate (aAPCert,
                                                                                MetaAS4Manager.getTimestampMgr ()
                                                                                              .getCurrentDateTime (),
                                                                                ETriState.FALSE,
                                                                                null);
    if (eCheckResult.isInvalid ())
    {
      // TODO Change from "true" to "false" once you have a Peppol
      // certificate so that an exception is thrown
      if (true)
        LOGGER.error ("The provided certificate is not a valid Peppol certificate. Check result: " + eCheckResult);
      else
      {
        throw new InitializationException ("The provided certificate is not a Peppol certificate. Check result: " +
                                           eCheckResult);
      }
    }
    else
      LOGGER.info ("Successfully checked that the provided Peppol AP certificate is valid.");

    // Must be set independent on the enabled/disable status
    Phase4PeppolDefaultReceiverConfiguration.setAPCAChecker (aAPCAChecker);

    // Eventually enable the receiver check, so that for each incoming request
    // the validity is crosscheck against the owning SMP
    final String sSMPURL = APConfig.getMySmpUrl ();
    final String sAPURL = AS4Configuration.getThisEndpointAddress ();
    if (StringHelper.isNotEmpty (sSMPURL) && StringHelper.isNotEmpty (sAPURL))
    {
      // To process the message even though the receiver is not registered in
      // our AP
      Phase4PeppolDefaultReceiverConfiguration.setReceiverCheckEnabled (true);
      Phase4PeppolDefaultReceiverConfiguration.setSMPClient (new SMPClientReadOnly (URLHelper.getAsURI (sSMPURL)));
      Phase4PeppolDefaultReceiverConfiguration.setAS4EndpointURL (sAPURL);
      Phase4PeppolDefaultReceiverConfiguration.setAPCertificate (aAPCert);
      LOGGER.info ("phase4 Peppol receiver checks are enabled");
    }
    else
    {
      Phase4PeppolDefaultReceiverConfiguration.setReceiverCheckEnabled (false);
      LOGGER.warn ("phase4 Peppol receiver checks are disabled");
    }

    // Initialize the Reporting Backend only once
    if (PeppolReportingBackend.getBackendService ().initBackend (APConfig.getConfig ()).isFailure ())
      throw new InitializationException ("Failed to init Peppol Reporting Backend Service");
  }

  // At 05:00 AM, on day 2 of the month
  @Scheduled (cron = "0 0 5 2 * *")
  public void sendPeppolReportingMessages ()
  {
    if (APConfig.isSchedulePeppolReporting ())
    {
      LOGGER.info ("Running scheduled creation and sending of Peppol Reporting messages");
      // Use the previous month
      final YearMonth aYearMonth = YearMonth.now ().minusMonths (1);
      AppReportingHelper.createAndSendPeppolReports (aYearMonth);
    }
    else
      LOGGER.warn ("Creating and sending Peppol Reports is disabled in the configuration");
  }

  /**
   * Special class that is only present to have a graceful shutdown. The the bean method below.
   *
   * @author Philip Helger
   */
  private static final class Destroyer
  {
    @PreDestroy
    public void destroy ()
    {
      if (WebScopeManager.isGlobalScopePresent ())
      {
        // Shutdown the Peppol Reporting Backend service, if it was initialized
        final IPeppolReportingBackendSPI aPRBS = PeppolReportingBackend.getBackendService ();
        if (aPRBS != null && aPRBS.isInitialized ())
          aPRBS.shutdownBackend ();

        AS4ServerInitializer.shutdownAS4Server ();
        WebFileIO.resetPaths ();
        WebScopeManager.onGlobalEnd ();
      }
    }
  }

  @Bean
  public Destroyer destroyer ()
  {
    return new Destroyer ();
  }
}
