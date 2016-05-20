package com.braintreepayments.testutils;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JSONBuilder {

    private JSONObject mJsonBody;

    public JSONBuilder() {
        mJsonBody = new JSONObject();
    }

    @SuppressWarnings("unchecked")
    public <T> T build(Class<T> clazz) {
        if (clazz.equals(String.class)) {
            return (T) build();
        } else {
            throw new RuntimeException("Expecting String");
        }
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
        try {
            mJsonBody.put(current.getMethodName(), value);
        } catch (JSONException ignored) {}
    }
}
