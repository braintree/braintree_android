package com.braintreepayments.api;

import org.json.JSONObject;

public class Json {

    /**
     * Returns the value mapped by name if it exists, coercing it if necessary, or fallback if no such mapping exists.
     *
     * This is a work around for http://code.google.com/p/android/issues/detail?id=13830 returning "null" if the json
     * value is null.
     *
     * @param json
     * @param name
     * @param fallback
     * @return {@link String}
     */
    public static String optString(JSONObject json, String name, String fallback) {
        if (json.isNull(name)) {
            return fallback;
        } else {
            return json.optString(name, fallback);
        }
    }
}
