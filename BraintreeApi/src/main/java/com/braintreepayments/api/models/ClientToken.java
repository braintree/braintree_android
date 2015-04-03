package com.braintreepayments.api.models;

import android.util.Base64;

import com.google.gson.Gson;

import java.util.regex.Pattern;

/**
 * A class containing the configuration url and authorization for the current Braintree environment.
 */
public class ClientToken {

    private static final Pattern BASE_64_PATTERN = Pattern.compile(
            "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)");

    private String configUrl;
    private String authorizationFingerprint;

    /**
     * Create a new {@link ClientToken} instance from a client token
     *
     * @param clientToken A client token from the Braintree Gateway
     * @return {@link ClientToken} instance
     */
    public static ClientToken fromString(String clientToken) {
        if (BASE_64_PATTERN.matcher(clientToken).matches()) {
            clientToken = new String(Base64.decode(clientToken, Base64.DEFAULT));
        }

        return new Gson().fromJson(clientToken, ClientToken.class);
    }

    /**
     * @return The url to fetch configuration for the current Braintree environment.
     */
    public String getConfigUrl() {
        return configUrl;
    }

    /**
     * @return The authorizationFingerprint for the current session
     */
    public String getAuthorizationFingerprint() {
        return authorizationFingerprint;
    }
}
