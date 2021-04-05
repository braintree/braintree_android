package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

class PaymentMethodNonceFactory {
    static JSONObject extractPaymentMethodToken(String paymentDataString) throws JSONException {
        return new JSONObject(new JSONObject(paymentDataString)
                        .getJSONObject("paymentMethodData")
                        .getJSONObject("tokenizationData")
                        .getString("token"));
    }

    static PaymentMethodNonce fromString(String paymentDataString) throws JSONException {
        JSONObject token = extractPaymentMethodToken(paymentDataString);
        Iterator<String> keys = token.keys();

        while (keys.hasNext()) {
            String key = keys.next();

            switch (key) {
                case GooglePayCardNonce.API_RESOURCE_KEY:
                    return new GooglePayCardNonce(paymentDataString);

                case PayPalAccountNonce.API_RESOURCE_KEY:
                    return new PayPalAccountNonce(paymentDataString);
            }
        }

        throw new JSONException("Could not parse JSON for a payment method nonce");
    }
}
