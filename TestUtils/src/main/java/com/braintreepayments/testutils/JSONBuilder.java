package com.braintreepayments.testutils;

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
        StackTraceElement current = Thread.currentThread().getStackTrace()[2];
        try {
            mJsonBody.put(current.getMethodName(), value);
        } catch (JSONException ignored) {}
    }
}
