package com.braintreepayments.api;

import androidx.annotation.Nullable;

class AuthorizationLoader {

    private Authorization authorization;
    private final BraintreeAuthProvider authProvider;

    public AuthorizationLoader(BraintreeAuthProvider authProvider, String initialAuthString) {
        this.authProvider = authProvider;
        if (initialAuthString != null) {
            this.authorization = Authorization.fromString(initialAuthString);
        }
    }

    void loadAuthorization(final AuthorizationCallback callback) {
        if (authorization != null) {
            callback.onAuthorization(authorization, null);
        } else {
            authProvider.getAuthorization(new BraintreeAuthCallback() {
                @Override
                public void onAuthResult(@Nullable String authString, @Nullable Exception error) {
                    if (authString != null) {
                        authorization = Authorization.fromString(authString);
                        callback.onAuthorization(authorization, null);
                    } else {
                        callback.onAuthorization(null, error);
                    }
                }
            });
        }
    }

    public Authorization getAuthorization() {
        return authorization;
    }
}
