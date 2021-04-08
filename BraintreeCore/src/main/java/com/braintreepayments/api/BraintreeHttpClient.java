package com.braintreepayments.api;

import android.net.Uri;

import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.HttpClient.RetryStrategy;

import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

/**
 * Network request class that handles Braintree request specifics and threading.
 */
class BraintreeHttpClient {

    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CLIENT_KEY_HEADER = "Client-Key";

    private final HttpClient httpClient;
    private final Authorization authorization;

    BraintreeHttpClient(Authorization authorization) {
        this(authorization, new HttpClient(getSocketFactory(), new BraintreeHttpResponseParser()));
    }

    @VisibleForTesting
    BraintreeHttpClient(Authorization authorization, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.authorization = authorization;
    }

    private static SSLSocketFactory getSocketFactory() {
        try {
            return new TLSSocketFactory(BraintreeGatewayCertificate.getCertInputStream());
        } catch (SSLException e) {
            return null;
        }
    }

    Authorization getAuthorization() {
        return authorization;
    }

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via GET
     * @param configuration configuration for the Braintree Android SDK.
     * @param callback {@link HttpResponseCallback}
     */
    void get(String path, Configuration configuration, HttpResponseCallback callback) {
        get(path, configuration, HttpClient.NO_RETRY, callback);
    }

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via GET
     * @param configuration configuration for the Braintree Android SDK.
     * @param callback {@link HttpResponseCallback}
     * @param retryStrategy retry strategy
     */
    void get(String path, Configuration configuration, @RetryStrategy int retryStrategy, HttpResponseCallback callback) {
        boolean isRelativeURL = !path.startsWith("http");
        if (configuration == null && isRelativeURL) {
            String message = "Braintree HTTP GET request without configuration cannot have a relative path.";
            BraintreeException relativeURLNotAllowedError = new BraintreeException(message);
            callback.failure(relativeURLNotAllowedError);
            return;
        }

        String targetPath;
        if (authorization instanceof ClientToken || authorization instanceof PayPalUAT) {
            targetPath = Uri.parse(path)
                    .buildUpon()
                    .appendQueryParameter(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer())
                    .toString();
        } else {
            targetPath = path;
        }

        HttpRequest request = new HttpRequest()
                .method("GET")
                .path(targetPath)
                .addHeader(USER_AGENT_HEADER, "braintree/android/" + BuildConfig.VERSION_NAME);

        if (isRelativeURL && configuration != null) {
            request.baseUrl(configuration.getClientApiUrl());
        }

        if (authorization instanceof TokenizationKey) {
            request.addHeader(CLIENT_KEY_HEADER, authorization.getBearer());
        }

        httpClient.sendRequest(request, retryStrategy, callback);
    }

    /**
     * Make a HTTP POST request to Braintree.
     * If the path is a full url, it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @param callback {@link HttpResponseCallback}
     * @param configuration configuration for the Braintree Android SDK.
     */
    void post(String path, String data, Configuration configuration, HttpResponseCallback callback) {
        boolean isRelativeURL = !path.startsWith("http");
        if (configuration == null && isRelativeURL) {
            String message = "Braintree HTTP GET request without configuration cannot have a relative path.";
            BraintreeException relativeURLNotAllowedError = new BraintreeException(message);
            callback.failure(relativeURLNotAllowedError);
            return;
        }

        String requestData;
        if (authorization instanceof ClientToken) {
            try {
                requestData = new JSONObject(data)
                        .put(AUTHORIZATION_FINGERPRINT_KEY,
                                ((ClientToken) authorization).getAuthorizationFingerprint())
                        .toString();
            } catch (JSONException e) {
                callback.failure(e);
                return;
            }
        } else {
            requestData = data;
        }

        HttpRequest request = new HttpRequest()
                .method("POST")
                .path(path)
                .data(requestData)
                .addHeader(USER_AGENT_HEADER, "braintree/android/" + BuildConfig.VERSION_NAME);

        if (isRelativeURL && configuration != null) {
            request.baseUrl(configuration.getClientApiUrl());
        }

        if (authorization instanceof TokenizationKey) {
            request.addHeader(CLIENT_KEY_HEADER, authorization.getBearer());
        }
        httpClient.sendRequest(request, callback);
    }

    /**
     * Makes a synchronous HTTP POST request to Braintree.
     *
     * @param path the path or url to request from the server via HTTP POST
     * @param data the body of the post request
     * @param configuration configuration for the Braintree Android SDK.
     * @return the HTTP response body
     */
    String post(String path, String data, Configuration configuration) throws Exception {
        boolean isRelativeURL = !path.startsWith("http");
        if (configuration == null && isRelativeURL) {
            String message = "Braintree HTTP GET request without configuration cannot have a relative path.";
            throw new BraintreeException(message);
        }

        String requestData;
        if (authorization instanceof ClientToken) {
            requestData = new JSONObject(data)
                    .put(AUTHORIZATION_FINGERPRINT_KEY,
                            ((ClientToken) authorization).getAuthorizationFingerprint())
                    .toString();
        } else {
            requestData = data;
        }

        HttpRequest request = new HttpRequest()
                .method("POST")
                .path(path)
                .data(requestData)
                .addHeader(USER_AGENT_HEADER, "braintree/android/" + BuildConfig.VERSION_NAME);

        if (isRelativeURL && configuration != null) {
            request.baseUrl(configuration.getClientApiUrl());
        }

        if (authorization instanceof TokenizationKey) {
            request.addHeader(CLIENT_KEY_HEADER, authorization.getBearer());
        }
        return httpClient.sendRequest(request);
    }
}
