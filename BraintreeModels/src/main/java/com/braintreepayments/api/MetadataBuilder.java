package com.braintreepayments.api;

import com.braintreepayments.api.models.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

class MetadataBuilder {

    static final String META_KEY = "_meta";

    private static final String SOURCE_KEY = "source";
    private static final String INTEGRATION_KEY = "integration";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String VERSION_KEY = "version";
    private static final String PLATFORM_KEY = "platform";

    private JSONObject mJson;

    MetadataBuilder() {
        mJson = new JSONObject();
        try {
            mJson.put(PLATFORM_KEY, "android");
        } catch (JSONException ignored) {}
    }

    MetadataBuilder source(String source) {
        try {
            mJson.put(SOURCE_KEY, source);
        } catch (JSONException ignored) {}

        return this;
    }

    MetadataBuilder integration(String integration) {
        try {
            mJson.put(INTEGRATION_KEY, integration);
        } catch (JSONException ignored) {}

        return this;
    }

    MetadataBuilder sessionId(String sessionId) {
        try {
            mJson.put(SESSION_ID_KEY, sessionId);
        } catch (JSONException ignored) {}

        return this;
    }

    MetadataBuilder version() {
        try {
            mJson.put(VERSION_KEY, BuildConfig.VERSION_NAME);
        } catch (JSONException ignored) {}

        return this;
    }

    JSONObject build() {
        return mJson;
    }

    @Override
    public String toString() {
        return mJson.toString();
    }
}
