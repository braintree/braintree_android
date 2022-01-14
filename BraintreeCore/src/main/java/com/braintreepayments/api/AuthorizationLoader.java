package com.braintreepayments.api;

import androidx.annotation.Nullable;

class AuthorizationLoader {

    AuthorizationLoader(@Nullable String initialAuthString, @Nullable ClientTokenProvider clientTokenProvider) {

    }

    void loadAuthorization(AuthorizationCallback callback) {

    }

    @Nullable
    Authorization getAuthorizationFromCache() {
        return null;
    }
}
