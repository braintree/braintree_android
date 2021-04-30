package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class BraintreeGraphQLHttpClient {

    private final HttpClient httpClient;
    private final Authorization authorization;

    BraintreeGraphQLHttpClient(Authorization authorization) {
        this(authorization, new HttpClient(getSocketFactory(), new BraintreeGraphQLHttpResponseParser()));
    }

    @VisibleForTesting
    BraintreeGraphQLHttpClient(Authorization authorization, HttpClient httpClient) {
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
        if (authorization instanceof InvalidToken) {
            String message = ((InvalidToken) authorization).getErrorMessage();
            callback.failure(new BraintreeException(message));
            return;
        }

        HttpRequest request = new HttpRequest()
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
        if (authorization instanceof InvalidToken) {
            String message = ((InvalidToken) authorization).getErrorMessage();
            callback.failure(new BraintreeException(message));
            return;
        }

        HttpRequest request = new HttpRequest()
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
        if (authorization instanceof InvalidToken) {
            String message = ((InvalidToken) authorization).getErrorMessage();
            throw new BraintreeException(message);
        }

        HttpRequest request = new HttpRequest()
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
