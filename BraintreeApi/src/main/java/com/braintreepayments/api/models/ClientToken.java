package com.braintreepayments.api.models;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.regex.Pattern;

/**
 * A class containing the configuration url and authorization for the current Braintree environment.
 */
public class ClientToken {

    private static final Pattern BASE_64_PATTERN = Pattern.compile(
            "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)");

    @SerializedName("configUrl") private String mConfigUrl;
    @SerializedName("authorizationFingerprint") private String mAuthorizationFingerprint;

    // Used by buildPayPalAuthorizationConfiguration because
    // Future Payments browser switch requires client token.
    private String mOriginalValue;

    /**
     * Create a new {@link ClientToken} instance from a client token.
     *
     * @param clientTokenString A client token from the Braintree Gateway
     * @return {@link ClientToken} instance
     */
    public static ClientToken fromString(String clientTokenString) {
        if (BASE_64_PATTERN.matcher(clientTokenString).matches()) {
            clientTokenString = new String(Base64.decode(clientTokenString, Base64.DEFAULT));
        }
        ClientToken clientToken = new Gson().fromJson(clientTokenString, ClientToken.class);
        clientToken.mOriginalValue = clientTokenString;
        return clientToken;
    }

    /**
     * @return The url to fetch configuration for the current Braintree environment.
     */
    public String getConfigUrl() {
        return mConfigUrl;
    }

    /**
     * @return The authorizationFingerprint for the current session.
     */
    public String getAuthorizationFingerprint() {
        return mAuthorizationFingerprint;
    }

    /**
     * @return The original client token string.
     */
    public String getOriginalValue() {
        return mOriginalValue;
    }
}
