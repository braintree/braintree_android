package com.braintreepayments.api;

import static com.braintreepayments.api.LocalPaymentClient.LOCAL_PAYMENT_CANCEL;
import static com.braintreepayments.api.LocalPaymentClient.LOCAL_PAYMENT_SUCCESS;

import org.json.JSONException;
import org.json.JSONObject;

class LocalPaymentApi {

    private final BraintreeClient braintreeClient;

    LocalPaymentApi(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    void createPaymentMethod(final LocalPaymentRequest request, final LocalPaymentStartCallback callback) {
        String returnUrl = braintreeClient.getReturnUrlScheme() + "://" + LOCAL_PAYMENT_SUCCESS;
        String cancel = braintreeClient.getReturnUrlScheme() + "://" + LOCAL_PAYMENT_CANCEL;

        String url = "/v1/local_payments/create";
        braintreeClient.sendPOST(url, request.build(returnUrl, cancel), new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                if (responseBody != null) {
                    try {
                        JSONObject responseJson = new JSONObject(responseBody);
                        String redirectUrl = responseJson.getJSONObject("paymentResource").getString("redirectUrl");
                        String paymentToken = responseJson.getJSONObject("paymentResource").getString("paymentToken");

                        LocalPaymentResult transaction = new LocalPaymentResult(request, redirectUrl, paymentToken);
                        callback.onResult(transaction, null);
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, httpError);
                }
            }
        });
    }

    void tokenize(String merchantAccountId, String responseString, String clientMetadataID, final LocalPaymentBrowserSwitchResultCallback callback) {
        JSONObject payload = new JSONObject();

        try {
            payload.put("merchant_account_id", merchantAccountId);

            JSONObject paypalAccount = new JSONObject()
                    .put("intent", "sale")
                    .put("response", new JSONObject().put("webURL", responseString))
                    .put("options", new JSONObject().put("validate", false))
                    .put("response_type", "web")
                    .put("correlation_id", clientMetadataID);
            payload.put("paypal_account", paypalAccount);

            JSONObject metaData = new JSONObject()
                    .put("source", "client")
                    .put("integration", braintreeClient.getIntegrationType())
                    .put("sessionId", braintreeClient.getSessionId());
            payload.put("_meta", metaData);

            String url = "/v1/payment_methods/paypal_accounts";
            braintreeClient.sendPOST(url, payload.toString(), new HttpResponseCallback() {

                @Override
                public void onResult(String responseBody, Exception httpError) {
                    if (responseBody != null) {
                        try {
                            LocalPaymentNonce result = LocalPaymentNonce.fromJSON(new JSONObject(responseBody));
                            callback.onResult(result, null);
                        } catch (JSONException jsonException) {
                            callback.onResult(null, jsonException);
                        }
                    } else {
                        callback.onResult(null, httpError);
                    }
                }
            });
        } catch (JSONException ignored) { /* do nothing */ }
    }
}
