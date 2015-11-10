package com.braintreepayments.api.internal;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.api.models.ClientToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

/**
 * Network request class that handles Braintree request specifics and threading.
 */
public class BraintreeHttpClient {

    public static boolean DEBUG = false;

    private static final String TAG = "BraintreeHttpClient";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private static final String UTF_8 = "UTF-8";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";

    protected final MonitoredThreadPoolExecutor mThreadPool = MonitoredThreadPoolExecutor.newCachedThreadPool();

    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final TLSSocketFactory mTLSSocketFactory = new TLSSocketFactory();
    private final Authorization mAuthorization;
    private String mBaseUrl;
    private int mConnectTimeout = 30000; // 30 seconds
    private int mReadTimeout = 60000; // 60 seconds

    public BraintreeHttpClient(Authorization authorization) {
        mAuthorization = authorization;
    }

    public static String getUserAgent() {
        return "braintree/android/" + BuildConfig.VERSION_NAME;
    }

    public void setBaseUrl(String baseUrl) {
        mBaseUrl = (baseUrl == null) ? "" : baseUrl;
    }

    protected void setConnectTimeout(int timeout) {
        mConnectTimeout = timeout;
    }

    protected void setReadTimeout(int timeout) {
        mReadTimeout = timeout;
    }

    /**
     * Make a HTTP GET request to Braintree using the url and authorization fingerprint provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via GET
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    public void get(final String path, final HttpResponseCallback callback) {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                if (path == null) {
                    postCallbackOnMainThread(callback, new IllegalArgumentException("Path cannot be null"));
                    return;
                }

                Uri uri;
                if (path.startsWith("http")) {
                    uri = Uri.parse(path);
                } else {
                    uri = Uri.parse(mBaseUrl + path);
                }

                if (mAuthorization instanceof ClientToken) {
                    uri = uri.buildUpon()
                            .appendQueryParameter(AUTHORIZATION_FINGERPRINT_KEY,
                                    ((ClientToken) mAuthorization).getAuthorizationFingerprint())
                            .build();
                }

                HttpURLConnection connection = null;
                try {
                    connection = init(uri.toString());
                    connection.setRequestMethod(METHOD_GET);
                    handleResponse(connection, callback);
                } catch (IOException e) {
                    postCallbackOnMainThread(callback, e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    /**
     * Make a HTTP POST request to Braintree using the url and authorization fingerprint provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    public void post(final String path, final String data, final HttpResponseCallback callback) {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    String payload;
                    if (mAuthorization instanceof ClientToken) {
                        payload = new JSONObject(data)
                                .put(AUTHORIZATION_FINGERPRINT_KEY,
                                        ((ClientToken) mAuthorization).getAuthorizationFingerprint())
                                .toString();
                    } else {
                       payload = data;
                    }

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

                    handleResponse(connection, callback);
                } catch (IOException | JSONException e) {
                    postCallbackOnMainThread(callback, e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    protected HttpURLConnection init(String url) throws IOException {
        log("Opening url: " + url);

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(mTLSSocketFactory);
        }

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", getUserAgent());
        connection.setRequestProperty("Accept-Language", Locale.getDefault().getLanguage());

        if (mAuthorization instanceof TokenizationKey) {
            connection.setRequestProperty("Client-Key", mAuthorization.toString());
        }

        connection.setConnectTimeout(mConnectTimeout);
        connection.setReadTimeout(mReadTimeout);

        return connection;
    }

    private void handleResponse(HttpURLConnection connection, HttpResponseCallback callback)
            throws IOException {
        int responseCode = connection.getResponseCode();
        String responseBody;

        log("Received response code: " + responseCode);

        if (callback == null) {
            return;
        }

        switch(responseCode) {
            case HTTP_OK: case HTTP_CREATED: case HTTP_ACCEPTED:
                responseBody = readStream(connection.getInputStream());
                log("Received response body: " + responseBody);

                postCallbackOnMainThread(callback, responseBody);
                break;
            case HTTP_UNAUTHORIZED:
                postCallbackOnMainThread(callback, new AuthenticationException());
                break;
            case HTTP_FORBIDDEN:
                responseBody = readStream(connection.getErrorStream());
                log("Received error response body: " + responseBody);
                String errorMessage = new ErrorWithResponse(responseCode, responseBody).getMessage();

                postCallbackOnMainThread(callback, new AuthorizationException(errorMessage));
                break;
            case 422: // HTTP_UNPROCESSABLE_ENTITY
                responseBody = readStream(connection.getErrorStream());
                log("Received error response body: " + responseBody);

                postCallbackOnMainThread(callback, new ErrorWithResponse(responseCode, responseBody));
                break;
            case 426: // HTTP_UPGRADE_REQUIRED
                postCallbackOnMainThread(callback, new UpgradeRequiredException());
                break;
            case HTTP_INTERNAL_ERROR:
                postCallbackOnMainThread(callback, new ServerException());
                break;
            case HTTP_UNAVAILABLE:
                postCallbackOnMainThread(callback, new DownForMaintenanceException());
                break;
            default:
                postCallbackOnMainThread(callback, new UnexpectedException());
                break;
        }
    }

    private void postCallbackOnMainThread(final HttpResponseCallback callback, final String response) {
        if (callback == null) {
            return;
        }

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.success(response);
            }
        });
    }

    private void postCallbackOnMainThread(final HttpResponseCallback callback, final Exception exception) {
        if (callback == null) {
            return;
        }

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.failure(exception);
            }
        });
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
}
