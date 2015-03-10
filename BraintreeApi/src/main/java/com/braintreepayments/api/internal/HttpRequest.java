package com.braintreepayments.api.internal;

import android.util.Log;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.BraintreeSslException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;

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

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

public class HttpRequest {

    public static final String TAG = "HttpRequest";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String UTF_8 = "UTF-8";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";

    public static boolean DEBUG = false;

    private String mBaseUrl;
    private String mAuthorizationFingerprint;
    private int mConnectTimeout = 0;

    public HttpRequest(String baseUrl, String authorizationFingerprint) {
        mBaseUrl = (baseUrl == null) ? "" : baseUrl;
        mAuthorizationFingerprint = (authorizationFingerprint == null) ? "" : authorizationFingerprint;
    }

    public static String getUserAgent() {
        return  "braintree/android/" + BuildConfig.VERSION_NAME;
    }

    protected void setConnectTimeout(int timeout) {
        mConnectTimeout = timeout;
    }

    /**
     * Make a HTTP GET request to Braintree using the url and authorization fingerprint supplied in
     * the constructor. If the path is a ful url, it will be used instead of the url provided in the
     * constructor.
     *
     * @param path The path or url to request from the server via HTTP GET
     * @return {@link com.braintreepayments.api.internal.HttpResponse} containing the response code
     *         and body.
     * @throws com.braintreepayments.api.exceptions.ErrorWithResponse where there was a validation error.
     *         (Response code 422)
     * @throws com.braintreepayments.api.exceptions.BraintreeException when there was an error fulfilling
     *         the request.
     */
    public HttpResponse get(String path) throws ErrorWithResponse, BraintreeException {
        HttpURLConnection connection = null;
        try {
            String url = path + "?" + AUTHORIZATION_FINGERPRINT_KEY + "="
                    + URLEncoder.encode(mAuthorizationFingerprint, UTF_8);

            if (url.startsWith("http")) {
                connection = init(url);
            } else {
                connection = init(mBaseUrl + url);
            }

            connection.setRequestMethod(METHOD_GET);

            return parseResponse(connection);
        } catch (BraintreeException e) {
            throw e;
        } catch (IOException e) {
            throw new UnexpectedException(e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Make a HTTP POST request to Braintree using the url and authorization fingerprint supplied in
     * the constructor. If the path is a full url, it will be used instead of the url provided in the
     * constructor.
     *
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @return {@link com.braintreepayments.api.internal.HttpResponse} containing the response code
     *         and body.
     * @throws com.braintreepayments.api.exceptions.ErrorWithResponse when there was a validation error.
     * @throws com.braintreepayments.api.exceptions.BraintreeException when there was an error fulfilling
     *         the request.
     */
    public HttpResponse post(String path, String data) throws ErrorWithResponse, BraintreeException {
        HttpURLConnection connection = null;
        try {
            String payload = new JSONObject(data)
                    .put(AUTHORIZATION_FINGERPRINT_KEY, mAuthorizationFingerprint)
                    .toString();

            if (path.startsWith("http")) {
                connection = init(path);
            } else {
                connection = init(mBaseUrl + path);
            }

            connection.setRequestMethod(METHOD_POST);
            connection.setDoOutput(true);

            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(payload);
            out.flush();
            out.close();

            return parseResponse(connection);
        } catch (BraintreeException e) {
            throw e;
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

    protected HttpURLConnection init(String url) throws IOException {
        log("Opening url: " + url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(HttpRequest.getSslSocketFactory());
        }

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", HttpRequest.getUserAgent());
        connection.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());
        connection.setConnectTimeout(mConnectTimeout);

        return connection;
    }

    private HttpResponse parseResponse(HttpURLConnection connection)
            throws ErrorWithResponse, IOException {
        int responseCode = connection.getResponseCode();
        String responseBody;

        log("Received response code: " + responseCode);

        switch(responseCode) {
            case HTTP_OK: case HTTP_CREATED: case HTTP_ACCEPTED:
                responseBody = readStream(connection.getInputStream());
                log("Received response body: " + responseBody);

                return new HttpResponse(responseCode, responseBody);
            case HTTP_UNAUTHORIZED:
                throw new AuthenticationException();
            case HTTP_FORBIDDEN:
                throw new AuthorizationException();
            case 422: // HTTP_UNPROCESSABLE_ENTITY
                responseBody = readStream(connection.getErrorStream());
                log("Received error response body: " + responseBody);

                throw new ErrorWithResponse(responseCode, responseBody);
            case 426: // HTTP_UPGRADE_REQUIRED
                throw new UpgradeRequiredException();
            case HTTP_INTERNAL_ERROR:
                throw new ServerException();
            case HTTP_UNAVAILABLE:
                throw new DownForMaintenanceException();
            default:
                throw new UnexpectedException();
        }
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
