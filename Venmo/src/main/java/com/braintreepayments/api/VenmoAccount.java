package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

class VenmoAccount extends PaymentMethod {

    private static final String VENMO_ACCOUNT_KEY = "venmoAccount";
    private static final String NONCE_KEY = "nonce";

    private String nonce;
    private boolean shouldValidate;

    VenmoAccount() {}

    @Override
    JSONObject buildJSON() throws JSONException {
        JSONObject json = super.buildJSON();
        JSONObject paymentMethodNonceJson = new JSONObject();
        paymentMethodNonceJson.put(NONCE_KEY, nonce);

        JSONObject optionsJson = new JSONObject();
        optionsJson.put(VALIDATE_KEY, shouldValidate);
        paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson);

        json.put(VENMO_ACCOUNT_KEY, paymentMethodNonceJson);
        return json;
    }

    /**
     * @param shouldValidate Flag to denote when the associated {@link PaymentMethodNonce}
     *                       will be validated. When set to {@code true}, the {@link PaymentMethodNonce}
     *                       will be validated immediately. When {@code false}, the {@link PaymentMethodNonce}
     *                       will be validated when used by a server side library for a Braintree gateway action.
     */
    public void setShouldValidate(boolean shouldValidate) {
        this.shouldValidate = shouldValidate;
    }

    void setNonce(String nonce) {
        this.nonce = nonce;
    }

    @Override
    public String getApiPath() {
        return "venmo_accounts";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return VenmoAccountNonce.TYPE;
    }
}
