package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;

class BraintreeOptions {

    private final Context context;
    private final String integrationType;

    private String authorization;
    private ClientTokenProvider clientTokenProvider;
    private String sessionId;

    private String returnUrlScheme;

    BraintreeOptions(
            @NonNull Context context,
            @NonNull @IntegrationType.Integration String integrationType
    ) {
        this.context = context;
        this.integrationType = integrationType;
    }

    Context getContext() {
        return context;
    }

    String getIntegrationType() {
        return integrationType;
    }

    String getAuthorization() {
        return authorization;
    }

    BraintreeOptions authorization(String authorization) {
        this.authorization = authorization;
        return this;
    }

    String getSessionId() {
        return sessionId;
    }

    BraintreeOptions sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    String getReturnUrlScheme() {
        return returnUrlScheme;
    }

    BraintreeOptions returnUrlScheme(String returnUrlScheme) {
        this.returnUrlScheme = returnUrlScheme;
        return this;
    }

    ClientTokenProvider getClientTokenProvider() {
        return clientTokenProvider;
    }

    BraintreeOptions clientTokenProvider(ClientTokenProvider clientTokenProvider) {
        this.clientTokenProvider = clientTokenProvider;
        return this;
    }

    void setReturnUrlScheme(String returnUrlScheme) {
        this.returnUrlScheme = returnUrlScheme;
    }
}
