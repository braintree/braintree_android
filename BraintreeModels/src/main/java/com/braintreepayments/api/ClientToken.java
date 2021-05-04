package com.braintreepayments.api;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class containing the configuration url and authorization for the current Braintree environment.
 */
class ClientToken extends Authorization {

    static final String BASE_64_MATCHER =
            "([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";
    private static final String CONFIG_URL_KEY = "configUrl";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint";

    private final String configUrl;
    private final String authorizationFingerprint;

    /**
     * Create a new {@link ClientToken} instance from a client token
     *
     * @param clientTokenString A client token from the Braintree Gateway
     */
    ClientToken(String clientTokenString) throws InvalidArgumentException {
        super(clientTokenString);
        try {
            clientTokenString = new String(Base64.decode(clientTokenString, Base64.DEFAULT));

            JSONObject jsonObject = new JSONObject(clientTokenString);
            configUrl = jsonObject.getString(CONFIG_URL_KEY);
            authorizationFingerprint = jsonObject.getString(AUTHORIZATION_FINGERPRINT_KEY);
        } catch (NullPointerException | JSONException e) {
            throw new InvalidArgumentException("Client token was invalid");
        }
    }

    @Override
    String getConfigUrl() {
        return configUrl;
    }

    @Override
    String getBearer() {
        return authorizationFingerprint;
    }

    /**
     * @return The authorizationFingerprint for the current session
     */
    String getAuthorizationFingerprint() {
        return authorizationFingerprint;
    }

    /**
     * @return The customer ID in the authorizationFingerprint if it is present
     */
    String getCustomerId() {
        String authorizationFingerprint = getAuthorizationFingerprint();
        String[] components = authorizationFingerprint.split("&");
        for (String component : components) {
            if (component.contains("customer_id=")) {
                String[] customerComponents = component.split("=");
                if (customerComponents.length > 1) {
                    return customerComponents[1];
                }
            }
        }
        return null;
    }
}
