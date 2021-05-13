package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

class MetadataBuilder {

    static final String META_KEY = "_meta";

    private static final String SOURCE_KEY = "source";
    private static final String INTEGRATION_KEY = "integration";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String VERSION_KEY = "version";
    private static final String PLATFORM_KEY = "platform";

    private final JSONObject json;

    MetadataBuilder() {
        json = new JSONObject();
        try {
            json.put(PLATFORM_KEY, "android");
        } catch (JSONException ignored) {}
    }

    MetadataBuilder source(String source) {
        try {
            json.put(SOURCE_KEY, source);
        } catch (JSONException ignored) {}

        return this;
    }

    MetadataBuilder integration(String integration) {
        try {
            json.put(INTEGRATION_KEY, integration);
        } catch (JSONException ignored) {}

        return this;
    }

    MetadataBuilder sessionId(String sessionId) {
        try {
            json.put(SESSION_ID_KEY, sessionId);
        } catch (JSONException ignored) {}

        return this;
    }

    MetadataBuilder version() {
        try {
            json.put(VERSION_KEY, BuildConfig.VERSION_NAME);
        } catch (JSONException ignored) {}

        return this;
    }

    JSONObject build() {
        return json;
    }

    @Override
    public String toString() {
        return json.toString();
    }
}
