package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Contains configuration for Braintree analytics calls
 */
class AnalyticsConfiguration {

    private static final String URL_KEY = "url";

    private String mUrl;

    /**
     * Parse an {@link AnalyticsConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link AnalyticsConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    static AnalyticsConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        AnalyticsConfiguration analyticsConfiguration = new AnalyticsConfiguration();
        analyticsConfiguration.mUrl = Json.optString(json, URL_KEY, null);

        return analyticsConfiguration;
    }

    /**
     * @return {@link String} url of the Braintree analytics service.
     */
    String getUrl() {
        return mUrl;
    }

    /**
     * @return {@code true} if analytics are enabled, {@code false} otherwise.
     */
    boolean isEnabled() {
        return !TextUtils.isEmpty(mUrl);
    }
}
