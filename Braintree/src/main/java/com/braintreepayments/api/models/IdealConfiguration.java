package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.Json;

import org.json.JSONObject;

/**
 * Contains the remote iDEAL Configuration for the Braintree SDK.
 */
public class IdealConfiguration {

    private static final String ROUTE_ID_KEY = "routeId";
    private static final String ASSETS_URL_KEY = "assetsUrl";

    private String mRouteId;
    private String mAssetsUrl;

    static IdealConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        IdealConfiguration configuration = new IdealConfiguration();
        configuration.mRouteId = Json.optString(json, ROUTE_ID_KEY, "");
        configuration.mAssetsUrl = Json.optString(json, ASSETS_URL_KEY, "");
        return configuration;
    }

    /**
     * @return an identifier used to specify a processor connection.
     */
    public String getRouteId() {
        return mRouteId;
    }

    /**
     * @return a boolean indicating whether iDEAL is enabled for this merchant.
     */
    public boolean isEnabled() {
        return !TextUtils.isEmpty(mRouteId);
    }

    /**
     * @return the base URL for iDEAL assets.
     */
    public String getAssetsUrl() {
        return mAssetsUrl;
    }
}
