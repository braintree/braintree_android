package com.braintreepayments.api.models;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

/**
 * A class containing the configuration url and authorization for the current Braintree environment.
 */
public class ClientToken {

    private static final Pattern BASE_64_PATTERN = Pattern.compile(
            "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)");
    private static final String CONFIG_URL_KEY = "configUrl";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";

    private String mConfigUrl;
    private String mAuthorizationFingerprint;

    /**
     * Create a new {@link ClientToken} instance from a client token
     *
     * @param clientTokenString A client token from the Braintree Gateway
     * @return {@link ClientToken} instance
     */
    public static ClientToken fromString(String clientTokenString) throws JSONException {
        if (BASE_64_PATTERN.matcher(clientTokenString).matches()) {
            clientTokenString = new String(Base64.decode(clientTokenString, Base64.DEFAULT));
        }

        JSONObject jsonObject = new JSONObject(clientTokenString);
        ClientToken clientToken = new ClientToken();
        clientToken.mConfigUrl = jsonObject.getString(CONFIG_URL_KEY);
        clientToken.mAuthorizationFingerprint = jsonObject.getString(AUTHORIZATION_FINGERPRINT_KEY);

        return clientToken;
    }

    /**
     * @return The url to fetch configuration for the current Braintree environment.
     */
    public String getConfigUrl() {
        return mConfigUrl;
    }

    /**
     * @return The authorizationFingerprint for the current session
     */
    public String getAuthorizationFingerprint() {
        return mAuthorizationFingerprint;
    }
}
