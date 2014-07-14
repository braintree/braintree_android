package com.braintreepayments.api.internal;

import com.braintreepayments.api.internal.HttpRequest.HttpMethod;
import com.squareup.okhttp.OkHttpClient;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class HttpRequestFactory {

    private OkHttpClient mOkHttpClient;

    public HttpRequestFactory() {
        mOkHttpClient = getAndConfigureHttpClient();
    }

    public HttpRequest getRequest(HttpMethod method, String url) {
        return new HttpRequest(mOkHttpClient, method, url);
    }

    private OkHttpClient getAndConfigureHttpClient() {
        OkHttpClient httpClient = new OkHttpClient();
        httpClient.setSslSocketFactory(getSslSocketFactory());

        return httpClient;
    }

    /**
     * @return
     * @see <a href="http://developer.android.com/training/articles/security-ssl.html#UnknownCa">Android
     * Documentation</a>
     * @see <a href="https://github.com/braintree/braintree_java/blob/95b96c356324d1532714f849402f830251ce8b81/src/main/java/com/braintreegateway/util/Http.java#L100">Braintree
     * Java Client Library</a>
     */
    private SSLSocketFactory getSslSocketFactory() throws BraintreeSslException {
        PRNGFixes.apply();
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certStream = BraintreeGatewayCertificate.getCertInputStream();

            Collection<? extends Certificate> certificates = cf.generateCertificates(certStream);
            for (Certificate cert : certificates) {
                if (cert instanceof X509Certificate) {
                    X509Certificate x509cert = (X509Certificate) cert;
                    Principal principal = x509cert.getSubjectDN();
                    String subject = principal.getName();
                    keyStore.setCertificateEntry(subject, cert);
                }
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, null);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
                    SecureRandom.getInstance("SHA1PRNG"));

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new BraintreeSslException(e);
        }
    }

    public static class BraintreeSslException extends RuntimeException {
        BraintreeSslException(Exception e) {
            super(e);
        }
    }

}
