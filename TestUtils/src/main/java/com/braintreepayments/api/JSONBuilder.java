package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JSONBuilder {

    protected JSONObject jsonBody;

    protected JSONBuilder(JSONObject json) {
        jsonBody = json;
    }

    public JSONBuilder() {
        jsonBody = new JSONObject();
    }

    public String build() {
        return jsonBody.toString();
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
            jsonBody.put(key, value);
        } catch (JSONException ignored) {}
    }
}
