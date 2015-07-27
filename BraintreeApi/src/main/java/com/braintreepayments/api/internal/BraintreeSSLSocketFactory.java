package com.braintreepayments.api.internal;

import com.braintreepayments.api.exceptions.BraintreeSslException;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/* package */ class BraintreeSSLSocketFactory {

    /**
     * @return {@link SSLSocketFactory}
     * @see <a href="http://developer.android.com/training/articles/security-ssl.html#UnknownCa">Android Documentation</a>
     * @see <a href="https://github.com/braintree/braintree_java/blob/95b96c356324d1532714f849402f830251ce8b81/src/main/java/com/braintreegateway/util/Http.java#L100">Braintree Java Client Library</a>
     */
    protected static SSLSocketFactory getSslSocketFactory() throws BraintreeSslException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certStream = BraintreeGatewayCertificate.getCertInputStream();

            Collection<? extends Certificate> certificates = cf.generateCertificates(certStream);
            for (Certificate cert : certificates) {
                if (cert instanceof X509Certificate) {
                    String subject = ((X509Certificate) cert).getSubjectDN().getName();
                    keyStore.setCertificateEntry(subject, cert);
                }
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new BraintreeSslException(e);
        }
    }
}
