package com.braintreepayments.api.models;

import com.braintreepayments.api.Json;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents response received when creating a PayPal payment resource.
 */
public class PayPalTwoFactorAuthResponse {

    private static final String PAYMENT_TOKEN_KEY = "paymentToken";
    private static final String INTENT_KEY = "intent";
    private static final String REDIRECT_URL_KEY = "redirectUrl";
    private static final String AUTHENTICATE_URL_KEY = "authenticateUrl";

    private String mPaymentToken;
    private String mIntent;
    private String mRedirectUrl;
    private String mAuthenticateUrl;
    private String mAuthorizationFingerprint;

    /**
     * Construct a PayPal two factor authentication lookup response from JSON.
     * @param string JSON representation of a PayPalTwoFactorAuthResponse
     * @param authorizationFingerprint bearer token used to authorize the request
     * @return PayPalTwoFactorAuthResponse
     * @throws JSONException
     */
    public static PayPalTwoFactorAuthResponse fromJson(String string,
                                                       String authorizationFingerprint) throws JSONException {
        PayPalTwoFactorAuthResponse response = new PayPalTwoFactorAuthResponse();

        JSONObject json = new JSONObject(string).getJSONObject("paymentResource");
        response.mPaymentToken = Json.optString(json, PAYMENT_TOKEN_KEY, null);
        response.mIntent = Json.optString(json, INTENT_KEY, null);
        response.mRedirectUrl = Json.optString(json, REDIRECT_URL_KEY, null);
        response.mAuthenticateUrl = Json.optString(json, AUTHENTICATE_URL_KEY, null);
        response.mAuthorizationFingerprint = authorizationFingerprint;

        return response;
    }

    /**
     * Authentication url used for payment two factor authentication.
     */
    public String getAuthenticateUrl() {
        return mAuthenticateUrl;
    }

    /**
     * Token representing newly created PayPal payment resource.  This has no correlation with a Payment method token or nonce.
     */
    public String getPaymentToken() {
        return mPaymentToken;
    }

    /**
     * Intent of the resulting transaction the nonce will be used in.
     */
    public String getResourceIntent() {
        return mIntent;
    }

    /**
     * Redirect url.
     */
    public String getRedirectUrl() {
        return mRedirectUrl;
    }

    /**
     * Format the response as a JSON payload.
     * @return response formatted as JSON.
     */
    public String toJson(String correlationId) {
        JSONObject parameters = new JSONObject();
        try {
            boolean scaAuthenticationComplete = (mAuthenticateUrl == null);

            parameters
                    .put("paypal_account", new JSONObject()
                            .put("correlation_id", correlationId)
                            .put("payment_token", mPaymentToken)
                            .put("options", new JSONObject()
                                    .put("sca_authentication_complete", scaAuthenticationComplete)))
                    .put("authorization_fingerprint", mAuthorizationFingerprint);

        } catch (JSONException ignored) { }
        return parameters.toString();
    }
}
