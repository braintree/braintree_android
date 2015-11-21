package com.braintreepayments.api.internal;

import com.braintreepayments.api.exceptions.BraintreeSslException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

class TLSSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory mInternalSSLSocketFactory;

    /**
     * {@link SSLSocketFactory}
     * @see <a href="http://developer.android.com/training/articles/security-ssl.html#UnknownCa">Android Documentation</a>
     * @see <a href="https://github.com/braintree/braintree_java/blob/95b96c356324d1532714f849402f830251ce8b81/src/main/java/com/braintreegateway/util/Http.java#L100">Braintree Java Client Library</a>
     */
    TLSSocketFactory() throws BraintreeSslException {
        InputStream certStream = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certStream = BraintreeGatewayCertificate.getCertInputStream();

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
            mInternalSSLSocketFactory = sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new BraintreeSslException(e);
        } finally {
            try {
                certStream.close();
            } catch (IOException | NullPointerException ignored) {}
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mInternalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mInternalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(mInternalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTLSOnSocket(mInternalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return enableTLSOnSocket(
                mInternalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(mInternalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(
                mInternalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if (socket instanceof SSLSocket) {
            ArrayList<String> supportedProtocols =
                    new ArrayList<>(Arrays.asList(((SSLSocket) socket).getSupportedProtocols()));
            supportedProtocols.retainAll(Arrays.asList("TLSv1.2", "TLSv1.1", "TLSv1"));

            ((SSLSocket)socket).setEnabledProtocols(supportedProtocols.toArray(
                    new String[supportedProtocols.size()]));
        }

        return socket;
    }
}
