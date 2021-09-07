package com.braintreepayments.api.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

class CertificateHelper {

    byte[] getEncodedCertificate(byte[] signatureBytes) throws CertificateException {
        InputStream certStream = new ByteArrayInputStream(signatureBytes);
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);
        return x509Cert.getEncoded();
    }
}