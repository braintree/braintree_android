package com.braintreepayments.api.models;

import com.braintreepayments.api.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;


public class BraintreeMetadataBuilder {

    public static final String META_KEY = "_meta";

    private static final String SOURCE_KEY = "source";
    private static final String INTEGRATION_KEY = "integration";
    private static final String SESSION_ID_KEY = "sessionId";
    private static final String VERSION_KEY = "version";
    private static final String PLATFORM_KEY = "platform";

    private JSONObject mJson;

    public BraintreeMetadataBuilder() {
        mJson = new JSONObject();
    }

    public BraintreeMetadataBuilder source(String source) {
        try {
            mJson.put(SOURCE_KEY, source);
        } catch (JSONException ignored) {}

        return this;
    }

    public BraintreeMetadataBuilder integration(String integration) {
        try {
            mJson.put(INTEGRATION_KEY, integration);
        } catch (JSONException ignored) {}

        return this;
    }

    public BraintreeMetadataBuilder sessionId(String sessionId) {
        try {
            mJson.put(SESSION_ID_KEY, sessionId);
        } catch (JSONException ignored) {}

        return this;
    }

    public BraintreeMetadataBuilder version() {
        try {
            mJson.put(VERSION_KEY, BuildConfig.VERSION_NAME);
        } catch (JSONException ignored) {}

        return this;
    }

    public BraintreeMetadataBuilder platform() {
        try {
            mJson.put(PLATFORM_KEY, "android");
        } catch (JSONException ignored) {}

        return this;
    }

    public JSONObject build() {
        return mJson;
    }
}
