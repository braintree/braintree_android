package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

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

    void authenticateCardinalJWT(ThreeDSecureResult threeDSecureResult, String cardinalJWT, final ThreeDSecureResultCallback callback) {
        final CardNonce lookupCardNonce = threeDSecureResult.getTokenizedCard();
        final String lookupNonce = lookupCardNonce.getString();

        JSONObject body = new JSONObject();
        try {
            body.put("jwt", cardinalJWT);
            body.put("paymentMethodNonce", lookupNonce);
        } catch (JSONException ignored) {
        }

        String url = ApiClient.versionedPath(ApiClient.PAYMENT_METHOD_ENDPOINT + "/" + lookupNonce + "/three_d_secure/authenticate_from_jwt");
        String data = body.toString();

        braintreeClient.sendPOST(url, data, new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                if (responseBody != null) {
                    try {
                        ThreeDSecureResult result = ThreeDSecureResult.fromJson(responseBody);
                        if (result.hasError()) {
                            result.setTokenizedCard(lookupCardNonce);
                            braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.failure.returned-lookup-nonce");
                        } else {
                            braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.succeeded");
                        }
                        notify3DSComplete(result, callback);

                    } catch (JSONException e) {
                        callback.onResult(null, e);
                    }
                } else {
                    braintreeClient.sendAnalyticsEvent("three-d-secure.verification-flow.upgrade-payment-method.errored");
                    callback.onResult(null, httpError);
                }
            }
        });
    }

    private void notify3DSComplete(ThreeDSecureResult result, ThreeDSecureResultCallback callback) {
        ThreeDSecureInfo info = result.getTokenizedCard().getThreeDSecureInfo();

        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shifted.%b", info.isLiabilityShifted()));
        braintreeClient.sendAnalyticsEvent(String.format("three-d-secure.verification-flow.liability-shift-possible.%b", info.isLiabilityShiftPossible()));

        callback.onResult(result, null);
    }
}
