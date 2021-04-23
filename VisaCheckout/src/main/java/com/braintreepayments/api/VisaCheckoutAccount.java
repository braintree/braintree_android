package com.braintreepayments.api;

import com.visa.checkout.VisaPaymentSummary;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Use to construct a Visa Checkout tokenization request.
 */
class VisaCheckoutAccount extends PaymentMethod {
    private static final String CALL_ID = "callId";
    private static final String ENCRYPTED_KEY = "encryptedKey";
    private static final String ENCRYPTED_PAYMENT_DATA = "encryptedPaymentData";
    private static final String VISA_CHECKOUT_KEY = "visaCheckoutCard";

    private String callId;
    private String encryptedKey;
    private String encryptedPaymentData;

    /**
     * @param visaPaymentSummary returned from Visa Checkout after a successful payment.
     */
    VisaCheckoutAccount(VisaPaymentSummary visaPaymentSummary) {
        if (visaPaymentSummary == null) {
            return;
        }

        callId = visaPaymentSummary.getCallId();
        encryptedKey = visaPaymentSummary.getEncKey();
        encryptedPaymentData = visaPaymentSummary.getEncPaymentData();
    }

    @Override
    JSONObject buildTokenizationJSON() {
        JSONObject json = super.buildTokenizationJSON();
        JSONObject paymentMethodNonceJson = new JSONObject();

        try {
            paymentMethodNonceJson.put(CALL_ID, mCallId);
            paymentMethodNonceJson.put(ENCRYPTED_KEY, mEncryptedKey);
            paymentMethodNonceJson.put(ENCRYPTED_PAYMENT_DATA, mEncryptedPaymentData);
            json.put(VISA_CHECKOUT_KEY, paymentMethodNonceJson);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return json;
    }

    @Override
    protected void buildJSON(JSONObject base, JSONObject paymentMethodNonceJson) throws JSONException {
        paymentMethodNonceJson.put(CALL_ID, callId);
        paymentMethodNonceJson.put(ENCRYPTED_KEY, encryptedKey);
        paymentMethodNonceJson.put(ENCRYPTED_PAYMENT_DATA, encryptedPaymentData);
        base.put(VISA_CHECKOUT_KEY, paymentMethodNonceJson);
    }

    @Override
    protected void buildGraphQL(JSONObject base, JSONObject input) {}

    @Override
    public String getApiPath() {
        return "visa_checkout_cards";
    }

    @Override
    public String getResponsePaymentMethodType() {
        return VisaCheckoutNonce.TYPE;
    }
}
