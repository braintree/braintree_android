package com.braintreepayments.testutils;

import com.braintreepayments.api.testutils.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JSONBuilder {

    private JSONObject mJsonBody;

    public JSONBuilder() {
        mJsonBody = new JSONObject();
    }

    public String build() {
        return mJsonBody.toString();
    }

    public void put(Object value) {
        int stackIndex = 3;
        if (BuildConfig.FLAVOR.equals("unitTest")) {
            stackIndex--;
        }

        StackTraceElement current = Thread.currentThread().getStackTrace()[stackIndex];
        try {
            mJsonBody.put(current.getMethodName(), value);
        } catch (JSONException ignored) {}
    }
}
