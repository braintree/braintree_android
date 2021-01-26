package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.BuildConfig;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

public class BraintreeGraphQLHttpClient {

    private final HttpClient httpClient;
    private final Authorization authorization;

    public BraintreeGraphQLHttpClient(Authorization authorization) {
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

    public void post(String path, String data, Configuration configuration, HttpResponseCallback callback) {
        HttpRequest request = new HttpRequest()
                .method("POST")
                .path(path)
                .data(data)
                .baseUrl(configuration.getGraphQL().getUrl())
                .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
                .addHeader("Authorization", String.format("Bearer %s", authorization.getBearer()))
                .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION);
        httpClient.sendRequest(request, callback);
    }

    public void post(String data, Configuration configuration, HttpResponseCallback callback) {
        HttpRequest request = new HttpRequest()
                .method("POST")
                .path("")
                .data(data)
                .baseUrl(configuration.getGraphQL().getUrl())
                .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
                .addHeader("Authorization", String.format("Bearer %s", authorization.getBearer()))
                .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION);
        httpClient.sendRequest(request, callback);
    }

    public String post(String path, String data, Configuration configuration) throws Exception {
        HttpRequest request = new HttpRequest()
                .method("POST")
                .path(path)
                .data(data)
                .baseUrl(configuration.getGraphQL().getUrl())
                .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
                .addHeader("Authorization", String.format("Bearer %s", authorization.getBearer()))
                .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION);
        return httpClient.sendRequest(request);
    }
}
