package com.braintreepayments.api.threedsecure

import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.ApiClient.Companion.versionedPath
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.threedsecure.ThreeDSecureParams.Companion.fromJson
import org.json.JSONException
import org.json.JSONObject

internal class ThreeDSecureAPI(
    private val braintreeClient: BraintreeClient
) {

    fun performLookup(
        request: ThreeDSecureRequest,
        cardinalConsumerSessionId: String?,
        callback: ThreeDSecureResultCallback
    ) {
        braintreeClient.sendPOST(
            url = versionedPath(
                "${ApiClient.PAYMENT_METHOD_ENDPOINT}/${request.nonce}/three_d_secure/lookup"
            ),
            data = request.build(cardinalConsumerSessionId)
        ) { responseBody: String?, httpError: Exception? ->
            if (responseBody != null) {
                try {
                    val result: ThreeDSecureParams = fromJson(responseBody)
                    callback.onThreeDSecureResult(result, null)
                } catch (e: JSONException) {
                    callback.onThreeDSecureResult(null, e)
                }
            } else {
                callback.onThreeDSecureResult(null, httpError)
            }
        }
    }

    fun authenticateCardinalJWT(
        threeDSecureParams: ThreeDSecureParams?,
        cardinalJWT: String?,
        callback: ThreeDSecureResultCallback
    ) {
        if (threeDSecureParams == null || cardinalJWT == null) {
            callback.onThreeDSecureResult(null, BraintreeException("threeDSecureParams or jwt is null"))
        }

        val lookupCardNonce = threeDSecureParams?.threeDSecureNonce
        val lookupNonce = threeDSecureParams?.threeDSecureNonce?.string

        val body = JSONObject()
        try {
            body.put("jwt", cardinalJWT)
            body.put("paymentMethodNonce", lookupNonce)
        } catch (ignored: JSONException) {
        }

        val url = versionedPath(
            "${ApiClient.PAYMENT_METHOD_ENDPOINT}/$lookupNonce/three_d_secure/authenticate_from_jwt"
        )

        braintreeClient.sendPOST(
            url = url,
            data = body.toString()
        ) { responseBody: String?, httpError: Exception? ->
            if (responseBody != null) {
                try {
                    val result: ThreeDSecureParams = fromJson(responseBody)
                    if (result.hasError()) {
                        result.threeDSecureNonce = lookupCardNonce
                    }
                    callback.onThreeDSecureResult(result, null)
                } catch (e: JSONException) {
                    callback.onThreeDSecureResult(null, e)
                }
            } else {
                callback.onThreeDSecureResult(null, httpError)
            }
        }
    }
}
