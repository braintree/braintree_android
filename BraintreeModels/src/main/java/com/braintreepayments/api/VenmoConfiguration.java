package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Contains the remote Venmo configuration for the Braintree SDK.
 */
class VenmoConfiguration {

    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String ENVIRONMENT_KEY = "environment";
    private static final String MERCHANT_ID_KEY = "merchantId";

    private String accessToken;
    private String environment;
    private String merchantId;

    /**
     * Parses the Venmo configuration from json.
     *
     * @param json The json to parse.
     * @return A {@link VenmoConfiguration} instance with data that was able to be parsed from the
     * {@link JSONObject}.
     */
    static VenmoConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        VenmoConfiguration venmoConfiguration = new VenmoConfiguration();
        venmoConfiguration.accessToken = Json.optString(json, ACCESS_TOKEN_KEY, "");
        venmoConfiguration.environment = Json.optString(json, ENVIRONMENT_KEY, "");
        venmoConfiguration.merchantId = Json.optString(json, MERCHANT_ID_KEY, "");

        return venmoConfiguration;
    }

    /**
     * @return The access token to use with Venmo.
     */
    String getAccessToken() {
        return accessToken;
    }

    /**
     * @return The merchant Id associated with this merchant's Venmo integration.
     */
    String getMerchantId() {
        return merchantId;
    }

    /**
     * @return The Venmo environment the merchant is running in.
     */
    String getEnvironment() {
        return environment;
    }

    boolean isAccessTokenValid() {
        return !TextUtils.isEmpty(accessToken);
    }
}
