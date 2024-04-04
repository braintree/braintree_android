package com.braintreepayments.api.threedsecure;

import com.braintreepayments.api.core.ApiClient;
import com.braintreepayments.api.BraintreeClient;

import org.json.JSONException;
import org.json.JSONObject;

class ThreeDSecureAPI {

    private final BraintreeClient braintreeClient;

    ThreeDSecureAPI(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    void performLookup(final ThreeDSecureRequest request, String cardinalConsumerSessionId,
                       final ThreeDSecureResultCallback callback) {
        String url = ApiClient.versionedPath(
                ApiClient.PAYMENT_METHOD_ENDPOINT + "/" + request.getNonce() +
                        "/three_d_secure/lookup");
        String data = request.build(cardinalConsumerSessionId);

        braintreeClient.sendPOST(url, data, (responseBody, httpError) -> {
            if (responseBody != null) {
                try {
                    ThreeDSecureParams result = ThreeDSecureParams.fromJson(responseBody);
                    callback.onThreeDSecureResult(result, null);
                } catch (JSONException e) {
                    callback.onThreeDSecureResult(null, e);
                }
            } else {
                callback.onThreeDSecureResult(null, httpError);
            }
        });
    }

    void authenticateCardinalJWT(ThreeDSecureParams threeDSecureParams, String cardinalJWT,
                                 final ThreeDSecureResultCallback callback) {
        final ThreeDSecureNonce lookupCardNonce = threeDSecureParams.getThreeDSecureNonce();

        final String lookupNonce = lookupCardNonce.getString();

        JSONObject body = new JSONObject();
        try {
            body.put("jwt", cardinalJWT);
            body.put("paymentMethodNonce", lookupNonce);
        } catch (JSONException ignored) {
        }

        String url = ApiClient.versionedPath(ApiClient.PAYMENT_METHOD_ENDPOINT + "/" + lookupNonce +
                "/three_d_secure/authenticate_from_jwt");
        String data = body.toString();

        braintreeClient.sendPOST(url, data, (responseBody, httpError) -> {
            if (responseBody != null) {
                try {
                    ThreeDSecureParams result = ThreeDSecureParams.fromJson(responseBody);
                    if (result.hasError()) {
                        result.setThreeDSecureNonce(lookupCardNonce);
                    }
                    callback.onThreeDSecureResult(result, null);

                } catch (JSONException e) {
                    callback.onThreeDSecureResult(null, e);
                }
            } else {
                callback.onThreeDSecureResult(null, httpError);
            }
        });
    }
}
