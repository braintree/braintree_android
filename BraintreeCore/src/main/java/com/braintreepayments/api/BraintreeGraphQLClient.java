package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

class BraintreeGraphQLClient {

    private final HttpClient httpClient;
    private final AuthorizationLoader authorizationLoader;

    BraintreeGraphQLClient(AuthorizationLoader authorizationLoader) {
        this(authorizationLoader, new HttpClient(getSocketFactory(), new BraintreeGraphQLResponseParser()));
    }

    @VisibleForTesting
    BraintreeGraphQLClient(AuthorizationLoader authorizationLoader, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.authorizationLoader = authorizationLoader;
    }

    private static SSLSocketFactory getSocketFactory() {
        try {
            return new TLSSocketFactory(BraintreeGraphQLCertificate.getCertInputStream());
        } catch (SSLException e) {
            return null;
        }
    }

    void post(final String path, final String data, final Configuration configuration, final HttpResponseCallback callback) {
        authorizationLoader.loadAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorization(@Nullable Authorization authorization, @Nullable Exception error) {
                if (authorization instanceof InvalidAuthorization) {
                    String message = ((InvalidAuthorization) authorization).getErrorMessage();
                    callback.onResult(null, new BraintreeException(message));
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
        });
    }

    void post(final String data, final Configuration configuration, final HttpResponseCallback callback) {
        authorizationLoader.loadAuthorization(new AuthorizationCallback() {
            @Override
            public void onAuthorization(@Nullable Authorization authorization, @Nullable Exception error) {
                if (authorization instanceof InvalidAuthorization) {
                    String message = ((InvalidAuthorization) authorization).getErrorMessage();
                    callback.onResult(null, new BraintreeException(message));
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
        });
    }

    String post(String path, String data, Configuration configuration, Authorization authorization) throws Exception {
        if (authorization instanceof InvalidAuthorization) {
            String message = ((InvalidAuthorization) authorization).getErrorMessage();
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
