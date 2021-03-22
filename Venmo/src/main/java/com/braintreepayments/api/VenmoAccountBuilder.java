package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

public class VenmoAccountBuilder extends PaymentMethodBuilder {

    private static final String VENMO_ACCOUNT_KEY = "venmoAccount";
    private static final String NONCE_KEY = "nonce";

    private String mNonce;

    public VenmoAccountBuilder() {}

    public void nonce(String nonce) {
        mNonce = nonce;
    }

    @Override
    protected void buildJSON(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException {
        paymentMethodNonceJson.put(NONCE_KEY, mNonce);
        base.put(VENMO_ACCOUNT_KEY, paymentMethodNonceJson);
    }

    @Override
    protected void buildGraphQL(JSONObject base, JSONObject input) {}

    @Override
    public String getApiPath() {
        return "venmo_accounts";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return VenmoAccountNonce.TYPE;
    }
}
