package com.paypal.android.networking.http;

import android.util.Log;

import com.paypal.android.sdk.onetouch.core.metadata.ad;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.OkHttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class OkHttpClientFactory {
    private static final String TAG = OkHttpClientFactory.class.getSimpleName();
    private static final String SSL_CONTEXT_PROTOCOL_ACCEPT_ALL = "TLSv1";

    /**
     * Generate an OkHttpClient used for network communication for API operations
     *
     * @param networkTimeout Network Timeout
     * @param isTrustAll To Trust All Connections
     * @param useSslPinning Whether to use SSL Pinning Ability. Generally used for sandbox and live
     * only.
     * @param userAgent User Agent String
     * @param baseUrl Base URL of the Request.
     * @return OkHttpClient instance
     */
    public static OkHttpClient getOkHttpClient(int networkTimeout, boolean isTrustAll,
            boolean useSslPinning, String userAgent, String baseUrl) {
        Log.d(TAG, "Creating okhttp client.  networkTimeout=" + networkTimeout + ", isTrustAll=" +
                isTrustAll +
                ", useSslPinning=" + useSslPinning + ", userAgent=" + userAgent + ", baseUrl=" +
                baseUrl);

        OkHttpClient client = new OkHttpClient().setConnectionSpecs(
                Arrays.asList(ConnectionSpec.MODERN_TLS));

        client.setConnectTimeout(Integer.valueOf(networkTimeout).longValue(), TimeUnit.SECONDS);
        client.interceptors().add(new UserAgentInterceptor(userAgent));
        try {
            // TODO unobfuscate this dyson TLSSocketFactory
            client.setSslSocketFactory(new ad());
            if (isTrustAll) {
                // Trust All Socket Factory
                client.setSslSocketFactory(getTrustAllSocketFactory());
                // All Host Name Verifier
                client.setHostnameVerifier(getAcceptAllHostNameVerifier());
            } else if (useSslPinning) {
                client.setCertificatePinner(getCertificatePinner(baseUrl));
            }
        } catch (NoSuchAlgorithmException | KeyManagementException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return client;
    }

    /**
     * Returns an SSLSocketFactory which Trusts all Connections
     *
     * @return SSLSocketFactory
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static SSLSocketFactory getTrustAllSocketFactory()
            throws NoSuchAlgorithmException, KeyManagementException {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                            String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                            String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance(SSL_CONTEXT_PROTOCOL_ACCEPT_ALL);
        sslContext.init(null, trustAllCerts, null);
        // Create an ssl socket factory with our all-trusting manager
        return sslContext.getSocketFactory();
    }

    /**
     * Returns the Certificate Pinner Object used for Pinning Certificates based on each host.
     *
     * @param baseUrl URL of the host you wish to pin
     * @return CertificatePinner
     * @throws URISyntaxException
     */
    private static CertificatePinner getCertificatePinner(String baseUrl)
            throws URISyntaxException {
        String host = getDomainName(baseUrl);
        return new CertificatePinner.Builder()
                .add(host, "sha1/u8I+KQuzKHcdrT6iTb30I70GsD0=")
                .add(host, "sha1/7Q3I1izTEynYgv4tw/zFENNNuxQ=")
                .add(host, "sha1/sYEIGhmkwJQf+uiVKMEkyZs0rMc=")
                .add(host, "sha1/gzF+YoVCU9bXeDGQ7JGQVumRueM=")
                .build();

    }

    /**
     * Returns an Accept All Host Name verifier. This is used for testing other than sandbox and
     * live.
     *
     * @return HostnameVerifier
     */
    private static HostnameVerifier getAcceptAllHostNameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }

    /**
     * Returns a Domain Name from the URL
     *
     * @param url URL
     * @return String domain name of given URL
     * @throws URISyntaxException
     */
    private static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

}
