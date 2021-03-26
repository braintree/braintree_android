package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

class TokenizationResult {

    private static final String PAYMENT_METHOD_NONCE_KEY = "nonce";

    private final String nonce;

    static TokenizationResult fromJson(String json) throws JSONException {
        return new TokenizationResult(json);
    }

    private TokenizationResult(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        nonce = jsonObject.getString(PAYMENT_METHOD_NONCE_KEY);
    }

    String getNonce() {
        return nonce;
    }
}
