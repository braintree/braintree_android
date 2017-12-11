package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.Json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains the remote GraphQL configuration for the Braintree SDK.
 */
public class GraphQLConfiguration {

    public static final String TOKENIZE_CREDIT_CARDS_FEATURE = "tokenize_credit_cards";

    private final static String URL_KEY = "url";
    private final static String FEATURES_KEY = "features";

    private String mUrl;
    private Set<String> mFeatures;

    /**
     * Parse a {@link GraphQLConfiguration} from json.
     *
     * @param json The {@link JSONObject} to parse.
     * @return a {@link GraphQLConfiguration} instance with the data that was able to be parsed from the {@link
     * JSONObject}.
     */
    public static GraphQLConfiguration fromJson(JSONObject json) {
        if (json == null) {
            json = new JSONObject();
        }

        GraphQLConfiguration graphQLConfiguration = new GraphQLConfiguration();
        graphQLConfiguration.mUrl = Json.optString(json, URL_KEY, "");
        graphQLConfiguration.mFeatures = parseJsonFeatures(json.optJSONArray(FEATURES_KEY));

        return graphQLConfiguration;
    }

    /**
     * @return {@code true} if GraphQL is enabled, {@code false} otherwise.
     */
    public boolean isEnabled() {
        return !TextUtils.isEmpty(mUrl);
    }

    /**
     * @return the GraphQL url.
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Check if a specific feature is enabled in the GraphQL API.
     *
     * @param feature The feature to check.
     * @return {@code true} if GraphQL is enabled and the feature is enabled, {@code false} otherwise.
     */
    public boolean isFeatureEnabled(String feature) {
        return isEnabled() && mFeatures.contains(feature);
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
