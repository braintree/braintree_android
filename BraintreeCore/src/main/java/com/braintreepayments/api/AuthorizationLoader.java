package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class AuthorizationLoader {

    private Authorization authorization;
    private final ClientTokenProvider clientTokenProvider;

    AuthorizationLoader(@Nullable String initialAuthString, @Nullable ClientTokenProvider clientTokenProvider) {
        this.clientTokenProvider = clientTokenProvider;
        if (initialAuthString != null) {
            this.authorization = Authorization.fromString(initialAuthString);
        }
    }

    void loadAuthorization(@NonNull final AuthorizationCallback callback) {
        if (authorization != null) {
            callback.onAuthorizationResult(authorization, null);
        } else if (clientTokenProvider != null) {
            clientTokenProvider.getClientToken(new ClientTokenCallback() {
                @Override
                public void onSuccess(@NonNull String clientToken) {
                    authorization = Authorization.fromString(clientToken);
                    callback.onAuthorizationResult(authorization, null);
                }

                @Override
                public void onFailure(@NonNull Exception error) {
                    callback.onAuthorizationResult(null, error);
                }
            });
        } else {
            String clientSDKSetupURL
                = "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#initialization";
            String message = String.format("Authorization required. See %s for more info.", clientSDKSetupURL);
            callback.onAuthorizationResult(null, new BraintreeException(message));
        }
    }

    @Nullable
    Authorization getAuthorizationFromCache() {
        return authorization;
    }
}
