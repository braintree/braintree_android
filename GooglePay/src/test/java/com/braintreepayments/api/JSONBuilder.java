package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JSONBuilder {

    protected JSONObject mJsonBody;

    protected JSONBuilder(JSONObject json) {
        mJsonBody = json;
    }

    public JSONBuilder() {
        mJsonBody = new JSONObject();
    }

    public String build() {
        return mJsonBody.toString();
    }

    public void put(Object value) {
        int stackIndex = 3;
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (!stack[0].isNativeMethod()) {
            stackIndex--;
        }

        StackTraceElement current = stack[stackIndex];
        put(current.getMethodName(), value);
    }

    public void put(String key, Object value) {
        try {
            mJsonBody.put(key, value);
        } catch (JSONException ignored) {}
    }
}
