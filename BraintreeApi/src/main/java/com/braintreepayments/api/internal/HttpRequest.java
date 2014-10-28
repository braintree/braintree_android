package com.braintreepayments.api.internal;

import android.util.Log;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.BraintreeSslException;
import com.braintreepayments.api.exceptions.UnexpectedException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class HttpRequest {

    public static boolean DEBUG = false;
    public static final String TAG = "HttpRequest";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    private static final String UTF_8 = "UTF-8";

    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";

    private String mAuthorizationFingerprint;

    public HttpRequest(String authorizationFingerprint) {
        mAuthorizationFingerprint = authorizationFingerprint;
    }

    protected HttpURLConnection init(String url) throws IOException {
        log("Opening url: " + url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(HttpRequest.getSslSocketFactory());
        }

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME);
        connection.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());

        return connection;
    }

    public HttpResponse get(String url) throws UnexpectedException {
        HttpURLConnection connection = null;
        try {
            url += "?" + AUTHORIZATION_FINGERPRINT_KEY + "="
                    + URLEncoder.encode(mAuthorizationFingerprint, UTF_8);
            connection = init(url);
            connection.setRequestMethod(METHOD_GET);

            return handleServerResponse(connection);
        } catch (IOException e) {
            throw new UnexpectedException(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public HttpResponse post(String url, String data) throws UnexpectedException {
        HttpURLConnection connection = null;
        try {
            String payload = new JSONObject(data)
                .put(AUTHORIZATION_FINGERPRINT_KEY, mAuthorizationFingerprint)
                .toString();

            connection = init(url);
            connection.setRequestMethod(METHOD_POST);
            connection.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(payload);
            out.flush();
            out.close();

            HttpResponse response = handleServerResponse(connection);
            response.setData(payload);
            return response;
        } catch (IOException e) {
            throw new UnexpectedException(e.getMessage());
        } catch (JSONException e) {
            throw new UnexpectedException(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpResponse handleServerResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        String responseBody;
        if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
            responseBody = readStream(connection.getInputStream());
        } else {
            responseBody = readStream(connection.getErrorStream());
        }

        log("Received response code: " + responseCode);
        log("Received response: " + responseBody);

        HttpResponse response = new HttpResponse(responseCode, responseBody);
        response.setUrl(connection.getURL().toString());
        return response;
    }

    private String readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int count; (count = in.read(buffer)) != -1; ) {
            out.write(buffer, 0, count);
        }
        return new String(out.toByteArray(), UTF_8);
    }

    private void log(String message) {
        if (DEBUG && BuildConfig.DEBUG) {
            Log.d(TAG, message);
        }
    }

    /**
     * @return {@link javax.net.ssl.SSLSocketFactory}
     * @see <a href="http://developer.android.com/training/articles/security-ssl.html#UnknownCa">Android Documentation</a>
     * @see <a href="https://github.com/braintree/braintree_java/blob/95b96c356324d1532714f849402f830251ce8b81/src/main/java/com/braintreegateway/util/Http.java#L100">Braintree Java Client Library</a>
     */
    private static SSLSocketFactory getSslSocketFactory() throws BraintreeSslException {
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
