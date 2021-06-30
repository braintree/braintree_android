package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Contains the remote Braintree API Configuration for the Braintree SDK.
 */
class BraintreeApiConfiguration {

    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String URL_KEY = "url";

    private String accessToken;
    private String url;

    static BraintreeApiConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        BraintreeApiConfiguration braintreeApiConfiguration = new BraintreeApiConfiguration();
        braintreeApiConfiguration.accessToken = Json.optString(json, ACCESS_TOKEN_KEY, "");
        braintreeApiConfiguration.url = Json.optString(json, URL_KEY, "");

        return braintreeApiConfiguration;
    }

    /**
     * @return The Access Token for Braintree API.
     */
    String getAccessToken() {
        return accessToken;
    }

    /**
     * @return the base url for accessing Braintree API.
     */
    String getUrl() {
        return url;
    }

    /**
     * @return a boolean indicating whether Braintree API is enabled for this merchant.
     */
    boolean isEnabled() {
        return !TextUtils.isEmpty(accessToken);
    }
}
