package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A PayPal payment resource
 */
class PayPalNativeCheckoutPaymentResource {

    private static final String PAYMENT_RESOURCE_KEY = "paymentResource";
    private static final String REDIRECT_URL_KEY = "redirectUrl";
    private static final String AGREEMENT_SETUP_KEY = "agreementSetup";
    private static final String APPROVAL_URL_KEY = "approvalUrl";

    private String redirectUrl;

    private void redirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    /**
     * The redirectUrl for the payment used by One Touch Core for authorization
     *
     * @return a redirect URL string containing an EC token
     */
    String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * Create a PayPalPaymentResource from a jsonString. Checks for keys associated with
     * Single Payment and Billing Agreement flows.
     *
     * @param jsonString a valid JSON string representing the payment resource
     * @return a PayPal payment resource
     * @throws JSONException when json input is invalid
     */
    static PayPalNativeCheckoutPaymentResource fromJson(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);

        PayPalNativeCheckoutPaymentResource payPalPaymentResource = new PayPalNativeCheckoutPaymentResource();
        JSONObject redirectJson = json.optJSONObject(PAYMENT_RESOURCE_KEY);
        if (redirectJson != null) {
            payPalPaymentResource.redirectUrl(Json.optString(redirectJson, REDIRECT_URL_KEY, ""));
        } else {
            redirectJson = json.optJSONObject(AGREEMENT_SETUP_KEY);
            payPalPaymentResource.redirectUrl(Json.optString(redirectJson, APPROVAL_URL_KEY, ""));
        }
        return payPalPaymentResource;
    }
}
