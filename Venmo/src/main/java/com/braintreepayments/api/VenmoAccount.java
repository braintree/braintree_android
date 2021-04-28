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
    JSONObject buildJSON() {
        JSONObject json = super.buildJSON();
        JSONObject paymentMethodNonceJson = new JSONObject();
        try {
            paymentMethodNonceJson.put(NONCE_KEY, nonce);

            JSONObject optionsJson = new JSONObject();
            try {
                optionsJson.put(VALIDATE_KEY, shouldValidate);
                paymentMethodNonceJson.put(OPTIONS_KEY, optionsJson);
            } catch (JSONException exception) {
                exception.printStackTrace();
            }

            json.put(VENMO_ACCOUNT_KEY, paymentMethodNonceJson);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
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
