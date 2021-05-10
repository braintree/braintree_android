package com.braintreepayments.api;

import android.text.TextUtils;

import com.braintreepayments.api.GraphQLConstants.Keys;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains the remote GraphQL configuration for the Braintree SDK.
 */
class GraphQLConfiguration {

    private String url;
    private Set<String> features;

    /**
     * Parse a {@link GraphQLConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return a {@link GraphQLConfiguration} instance with the data that was able to be parsed from the {@link
     * JSONObject}.
     */
    static GraphQLConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        GraphQLConfiguration graphQLConfiguration = new GraphQLConfiguration();
        graphQLConfiguration.url = Json.optString(json, Keys.URL, "");
        graphQLConfiguration.features = parseJsonFeatures(json.optJSONArray(Keys.FEATURES));

        return graphQLConfiguration;
    }

    /**
     * @return {@code true} if GraphQL is enabled, {@code false} otherwise.
     */
    boolean isEnabled() {
        return !TextUtils.isEmpty(url);
    }

    /**
     * @return the GraphQL url.
     */
    String getUrl() {
        return url;
    }

    /**
     * Check if a specific feature is enabled in the GraphQL API.
     *
     * @param feature The feature to check.
     * @return {@code true} if GraphQL is enabled and the feature is enabled, {@code false} otherwise.
     */
    boolean isFeatureEnabled(String feature) {
        return isEnabled() && features.contains(feature);
    }

    private static Set<String> parseJsonFeatures(JSONArray jsonArray) {
        Set<String> features = new HashSet<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                features.add(jsonArray.optString(i, ""));
            }
        }

        return features;
    }
}
