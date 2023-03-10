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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public JSONObject buildJSON() throws JSONException {
        JSONObject json = super.buildJSON();

        JSONObject paymentMethodNonceJson = new JSONObject();
        paymentMethodNonceJson.put(CALL_ID, callId);
        paymentMethodNonceJson.put(ENCRYPTED_KEY, encryptedKey);
        paymentMethodNonceJson.put(ENCRYPTED_PAYMENT_DATA, encryptedPaymentData);

        json.put(VISA_CHECKOUT_KEY, paymentMethodNonceJson);
        return json;
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public String getApiPath() {
        return "visa_checkout_cards";
    }
}
