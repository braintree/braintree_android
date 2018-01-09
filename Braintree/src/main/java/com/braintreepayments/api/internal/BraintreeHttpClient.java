package com.braintreepayments.api.internal;

import android.net.Uri;

import com.braintreepayments.api.core.BuildConfig;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnprocessableEntityException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.TokenizationKey;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.SSLException;

/**
 * Network request class that handles Braintree request specifics and threading.
 */
public class BraintreeHttpClient extends HttpClient {

    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";
    private static final String TOKENIZATION_KEY_HEADER_KEY = "Client-Key";

    private final Authorization mAuthorization;

    public BraintreeHttpClient(Authorization authorization) {
        mAuthorization = authorization;

        setUserAgent(getUserAgent());

        try {
            setSSLSocketFactory(new TLSSocketFactory(BraintreeGatewayCertificate.getCertInputStream()));
        } catch (SSLException e) {
            setSSLSocketFactory(null);
        }
    }

    /**
     * @return User Agent {@link String} for the current SDK version.
     */
    public static String getUserAgent() {
        return "braintree/android/" + BuildConfig.VERSION_NAME;
    }

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via GET
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    @Override
    public void get(String path, HttpResponseCallback callback) {
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

        super.get(uri.toString(), callback);
    }

    /**
     * Make a HTTP POST request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    @Override
    public void post(String path, String data, HttpResponseCallback callback) {
        try {
            if (mAuthorization instanceof ClientToken) {
                data = new JSONObject(data)
                        .put(AUTHORIZATION_FINGERPRINT_KEY,
                                ((ClientToken) mAuthorization).getAuthorizationFingerprint())
                        .toString();
            }

            super.post(path, data, callback);
        } catch (JSONException e) {
            postCallbackOnMainThread(callback, e);
        }
    }

    /**
     * Makes a synchronous HTTP POST request to Braintree using the base url, path, and authorization provided.
     * @see BraintreeHttpClient#post(String, String, HttpResponseCallback)
     *
     * @param path the path or url to request from the server via HTTP POST
     * @param data the body of the post request
     * @return the HTTP response body
     */
    public String post(String path, String data) throws Exception {
        if (mAuthorization instanceof ClientToken) {
            data = new JSONObject(data)
                    .put(AUTHORIZATION_FINGERPRINT_KEY, ((ClientToken) mAuthorization).getAuthorizationFingerprint())
                    .toString();
        }
        return super.post(path, data);
    }

    @Override
    protected HttpURLConnection init(String url) throws IOException {
        HttpURLConnection connection = super.init(url);

        if (mAuthorization instanceof TokenizationKey) {
            connection.setRequestProperty(TOKENIZATION_KEY_HEADER_KEY, mAuthorization.getBearer());
        }

        return connection;
    }

    @Override
    protected String parseResponse(HttpURLConnection connection) throws Exception {
        try {
            return super.parseResponse(connection);
        } catch (AuthorizationException | UnprocessableEntityException e) {
            if (e instanceof AuthorizationException) {
                String errorMessage = new ErrorWithResponse(403, e.getMessage()).getMessage();
                throw new AuthorizationException(errorMessage);
            } else {
                throw new ErrorWithResponse(422, e.getMessage());
            }
        }
    }
}
