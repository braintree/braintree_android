package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Contains the remote Braintree API Configuration for the Braintree SDK.
 */
public class BraintreeApiConfiguration {

    private static final String ACCESS_TOKEN_KEY = "accessToken";
    private static final String URL_KEY = "url";

    private String mAccessToken;
    private String mUrl;

    static BraintreeApiConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        BraintreeApiConfiguration braintreeApiConfiguration = new BraintreeApiConfiguration();
        braintreeApiConfiguration.mAccessToken = Json.optString(json, ACCESS_TOKEN_KEY, "");
        braintreeApiConfiguration.mUrl = Json.optString(json, URL_KEY, "");

        return braintreeApiConfiguration;
    }

    /**
     * @return The Access Token for Braintree API.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * @return the base url for accessing Braintree API.
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * @return a boolean indicating whether Braintree API is enabled for this merchant.
     */
    public boolean isEnabled() {
        return !TextUtils.isEmpty(mAccessToken);
    }
}
