package com.braintreepayments.api;

import org.json.JSONException;

class ThreeDSecureAPI {

    private final BraintreeClient braintreeClient;

    public ThreeDSecureAPI(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    void performLookup(final ThreeDSecureRequest request, String cardinalConsumerSessionId, final ThreeDSecureResultCallback callback) {
        String url = ApiClient.versionedPath(ApiClient.PAYMENT_METHOD_ENDPOINT + "/" + request.getNonce() + "/three_d_secure/lookup");
        String data = request.build(cardinalConsumerSessionId);

        braintreeClient.sendPOST(url, data, new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                if (responseBody != null) {
                    try {
                        ThreeDSecureResult result = ThreeDSecureResult.fromJson(responseBody);
                        callback.onResult(result, null);
                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    callback.onResult(null, httpError);
                }
            }
        });
    }

    void authenticateCardinalJWT() {

    }
}
