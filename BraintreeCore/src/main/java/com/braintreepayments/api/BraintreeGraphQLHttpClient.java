package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class BraintreeGraphQLHttpClient {

    private final HttpClient httpClient;

    BraintreeGraphQLHttpClient() {
        this(new HttpClient(getSocketFactory(), new BraintreeGraphQLHttpResponseParser()));
    }

    @VisibleForTesting
    BraintreeGraphQLHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    private static SSLSocketFactory getSocketFactory() {
        try {
            return new TLSSocketFactory(BraintreeGraphQLCertificate.getCertInputStream());
        } catch (SSLException e) {
            return null;
        }
    }

    void post(String path, String data, Configuration configuration, HttpResponseCallback callback, Authorization authorization) {
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

    void post(String data, Configuration configuration, HttpResponseCallback callback, Authorization authorization) {
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

    String post(String path, String data, Configuration configuration, Authorization authorization) throws Exception {
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
