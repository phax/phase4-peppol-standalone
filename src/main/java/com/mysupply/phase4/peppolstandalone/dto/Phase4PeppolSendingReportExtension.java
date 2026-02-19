package com.mysupply.phase4.peppolstandalone.dto;

import com.helger.json.IJsonObject;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phase4.peppol.Phase4PeppolSendingReport;
import com.helger.security.certificate.CertificateHelper;
import jakarta.annotation.Nonnull;

import java.security.cert.X509Certificate;

public class Phase4PeppolSendingReportExtension extends Phase4PeppolSendingReport {

    private X509Certificate m_aC2Cert;

    public Phase4PeppolSendingReportExtension(@Nonnull final ISMLInfo aSMLInfo) {
        super(aSMLInfo);
    }

    public boolean hasC2Cert() {
        return m_aC2Cert != null;
    }

    public X509Certificate getC2Cert() {
        return m_aC2Cert;
    }

    public void setC2Cert(@Nonnull final X509Certificate aC2Cert) {
        m_aC2Cert = aC2Cert;
    }

    @Override
    @Nonnull
    public IJsonObject getAsJsonObject()
    {
        final IJsonObject ret = super.getAsJsonObject ();
        if (hasC2Cert ()) {
            ret.add ("c2Cert", CertificateHelper.getPEMEncodedCertificate (getC2Cert ()) );
            ret.add ("c2CertSubjectDN", getC2Cert ().getSubjectX500Principal ().getName ());
            ret.add ("c2CertIssuerDN", getC2Cert ().getIssuerX500Principal ().getName ());
            ret.add ("c2CertSerialNumber", getC2Cert ().getSerialNumber ().toString ());
        }
        return ret;
    }
}
