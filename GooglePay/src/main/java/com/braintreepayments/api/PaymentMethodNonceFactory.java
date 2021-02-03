package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class PaymentMethodNonceFactory {
    public static JSONObject extractPaymentMethodToken(String paymentDataString) throws JSONException {
        return new JSONObject(new JSONObject(paymentDataString)
                        .getJSONObject("paymentMethodData")
                        .getJSONObject("tokenizationData")
                        .getString("token"));
    }

    public static PaymentMethodNonce fromString(String paymentDataString) throws JSONException {
        JSONObject token = extractPaymentMethodToken(paymentDataString);
        Iterator<String> keys = token.keys();

        while (keys.hasNext()) {
            String key = keys.next();

            switch (key) {
                case GooglePaymentCardNonce.API_RESOURCE_KEY:
                    return GooglePaymentCardNonce.fromJson(paymentDataString);

                case PayPalAccountNonce.API_RESOURCE_KEY:
                    return PayPalAccountNonce.fromJson(paymentDataString);
            }
        }

        throw new JSONException("Could not parse JSON for a payment method nonce");
    }
}
