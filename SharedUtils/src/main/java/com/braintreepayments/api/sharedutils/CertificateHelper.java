package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CertificateHelper {

    byte[] getEncodedCertificate(byte[] signatureBytes) throws CertificateException {
        InputStream certStream = new ByteArrayInputStream(signatureBytes);
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);
        return x509Cert.getEncoded();
    }
}
