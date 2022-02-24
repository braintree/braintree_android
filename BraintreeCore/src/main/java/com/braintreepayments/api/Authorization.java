package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.annotation.Nullable;

/**
 * Generic base class for Braintree authorization
 */
abstract class Authorization {

    private final String rawValue;

    Authorization(String rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Returns an {@link Authorization} of the correct type for a given {@link String}. If an
     * invalid authorization string is provided, an {@link InvalidAuthorization} will be returned.
     * Requests with an {@link InvalidAuthorization} will return a {@link BraintreeException} to the
     * {@link HttpResponseCallback}.
     *
     * @param authorizationString Given string to transform into an {@link Authorization}.
     * @return {@link Authorization}
     */
    static Authorization fromString(@Nullable String authorizationString) {
        try {
            if (isTokenizationKey(authorizationString)) {
                return new TokenizationKey(authorizationString);
            } else if (isClientToken(authorizationString)) {
                return new ClientToken(authorizationString);
            } else {
                String errorMessage = "Authorization provided is invalid: " + authorizationString;
                return new InvalidAuthorization(authorizationString, errorMessage);
            }
        } catch (InvalidArgumentException error) {
            return new InvalidAuthorization(authorizationString, error.getMessage());
        }
    }

    /**
     * @return The url to fetch configuration for the current Braintree environment.
     */
    abstract String getConfigUrl();

    /**
     * @return The authorization bearer string for authorizing requests.
     */
    abstract String getBearer();

    /**
     * @return The original Client token or Tokenization Key string, which can be used for serialization
     */
    @Override
    public String toString() {
        return rawValue;
    }

    static boolean isTokenizationKey(String tokenizationKey) {
        return !TextUtils.isEmpty(tokenizationKey) && tokenizationKey.matches(TokenizationKey.MATCHER);
    }

    static boolean isClientToken(String clientToken) {
        return !TextUtils.isEmpty(clientToken) && clientToken.matches(ClientToken.BASE_64_MATCHER);
    }
}
