package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains PayPal payment info.
 *
 * Currently used by checkoutWithPayPal to create a payment before the user authorizes it.
 */
public class PayPalPaymentResource {

    private String redirectUrl;

    public PayPalPaymentResource() {}

    public PayPalPaymentResource redirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
        return this;
    }

    /**
     * The redirectUrl for the payment used by One Touch Core for authorization
     *
     */
    public String getRedirectUrl() {
        return this.redirectUrl;
    }

    /**
     * Create a PayPalPaymentResource from a jsonString
     *
     */
    public static PayPalPaymentResource fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);

        String redirect = json.getJSONObject("paymentResource").getString("redirectUrl");
        PayPalPaymentResource payPalPaymentResource = new PayPalPaymentResource().redirectUrl(redirect);

        return payPalPaymentResource;
    }
}
