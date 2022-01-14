package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class AuthorizationLoader {

    private Authorization authorization;
    private final ClientTokenProvider clientTokenProvider;

    AuthorizationLoader(@Nullable String initialAuthString, @Nullable ClientTokenProvider clientTokenProvider) {
        this.clientTokenProvider = clientTokenProvider;
        this.authorization = Authorization.fromString(initialAuthString);
    }

    void loadAuthorization(@NonNull final AuthorizationCallback callback) {
        if (authorization != null) {
            callback.onAuthorizationResult(authorization, null);
        } else {
            clientTokenProvider.getClientToken(new ClientTokenCallback() {
                @Override
                public void onSuccess(@NonNull String clientToken) {
                    authorization = Authorization.fromString(clientToken);
                    callback.onAuthorizationResult(authorization, null);
                }

                @Override
                public void onFailure(@NonNull Exception exception) {
                    callback.onAuthorizationResult(null, exception);
                }
            });
        }
    }

    @Nullable
    Authorization getAuthorizationFromCache() {
        return authorization;
    }

    AuthorizationType getAuthorizationType() {
        if (authorization instanceof TokenizationKey) {
            return AuthorizationType.TOKENIZATION_KEY;
        }
        return AuthorizationType.CLIENT_TOKEN;
    }
}
