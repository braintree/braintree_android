package com.braintreepayments.api.models;

import android.text.TextUtils;

import com.braintreepayments.api.Json;

import org.json.JSONObject;

/**
 * Contains the remote GraphQL configuration for the Braintree SDK.
 */
public class GraphQLConfiguration {

    private final static String URL_KEY = "url";

    private String mUrl;

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
}
