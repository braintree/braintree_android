package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class BraintreeGraphQLClient {

    private final HTTPClient httpClient;
    private final Authorization authorization;

    BraintreeGraphQLClient(Authorization authorization) {
        this(authorization, new HTTPClient(getSocketFactory(), new BraintreeGraphQLResponseParser()));
    }

    @VisibleForTesting
    BraintreeGraphQLClient(Authorization authorization, HTTPClient httpClient) {
        this.httpClient = httpClient;
        this.authorization = authorization;
    }

    private static SSLSocketFactory getSocketFactory() {
        try {
            return new TLSSocketFactory(BraintreeGraphQLCertificate.getCertInputStream());
        } catch (SSLException e) {
            return null;
        }
    }

    void post(String path, String data, Configuration configuration, HttpResponseCallback callback) {
        if (authorization instanceof InvalidAuthorization) {
            String message = ((InvalidAuthorization) authorization).getErrorMessage();
            callback.onResult(null, new BraintreeException(message));
            return;
        }

        HTTPRequest request = new HTTPRequest()
                .method("POST")
                .path(path)
                .data(data)
                .baseUrl(configuration.getGraphQLUrl())
                .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
                .addHeader("Authorization", String.format("Bearer %s", authorization.getBearer()))
                .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION);
        httpClient.sendRequest(request, callback);
    }

    void post(String data, Configuration configuration, HttpResponseCallback callback) {
        if (authorization instanceof InvalidAuthorization) {
            String message = ((InvalidAuthorization) authorization).getErrorMessage();
            callback.onResult(null, new BraintreeException(message));
            return;
        }

        HTTPRequest request = new HTTPRequest()
                .method("POST")
                .path("")
                .data(data)
                .baseUrl(configuration.getGraphQLUrl())
                .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
                .addHeader("Authorization", String.format("Bearer %s", authorization.getBearer()))
                .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION);
        httpClient.sendRequest(request, callback);
    }

    String post(String path, String data, Configuration configuration) throws Exception {
        if (authorization instanceof InvalidAuthorization) {
            String message = ((InvalidAuthorization) authorization).getErrorMessage();
            throw new BraintreeException(message);
        }

        HTTPRequest request = new HTTPRequest()
                .method("POST")
                .path(path)
                .data(data)
                .baseUrl(configuration.getGraphQLUrl())
                .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
                .addHeader("Authorization", String.format("Bearer %s", authorization.getBearer()))
                .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION);
        return httpClient.sendRequest(request);
    }
}
