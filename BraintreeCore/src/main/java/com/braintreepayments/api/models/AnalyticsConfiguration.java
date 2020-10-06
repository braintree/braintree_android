package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.Json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains configuration for Braintree analytics calls
 */
public class AnalyticsConfiguration {

    private static final String URL_KEY = "url";

    private String mUrl;

    /**
     * Parse an {@link AnalyticsConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return An {@link AnalyticsConfiguration} instance with data that was able to be parsed from
     *         the {@link JSONObject}.
     */
    public static AnalyticsConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        AnalyticsConfiguration analyticsConfiguration = new AnalyticsConfiguration();
        analyticsConfiguration.mUrl = Json.optString(json, URL_KEY, null);

        return analyticsConfiguration;
    }

    /**
     * Serialize the {@link AnalyticsConfiguration} to json.
     *
     * @return {@link JSONObject} containing the {@link AnalyticsConfiguration}.
     */
    public JSONObject toJson() {
        try {
            return new JSONObject().put(URL_KEY, mUrl);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    /**
     * @return {@link String} url of the Braintree analytics service.
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * @return {@code true} if analytics are enabled, {@code false} otherwise.
     */
    public boolean isEnabled() {
        return !TextUtils.isEmpty(mUrl);
    }
}
