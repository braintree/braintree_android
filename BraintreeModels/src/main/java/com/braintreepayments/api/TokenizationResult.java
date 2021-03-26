package com.braintreepayments.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class TokenizationResult {

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";

    public JSONObject getJson() {
        return json;
    }

    private final JSONObject json;

    static TokenizationResult fromJson(String json) throws JSONException {
        return new TokenizationResult(json);
    }

    private TokenizationResult(String json) throws JSONException {
        this.json = new JSONObject(json);
    }
}
