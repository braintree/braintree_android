package com.braintreepayments.api.threedsecure

import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.ApiClient.Companion.versionedPath
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.threedsecure.ThreeDSecureParams.Companion.fromJson
import org.json.JSONException
import org.json.JSONObject

internal class ThreeDSecureAPI(
    private val braintreeClient: BraintreeClient,
) {
    suspend fun performLookup(
        request: ThreeDSecureRequest,
        cardinalConsumerSessionId: String?
    ): ThreeDSecureParams {
        val responseBody = braintreeClient.sendPOST(
            url = versionedPath(
                "${ApiClient.PAYMENT_METHOD_ENDPOINT}/${request.nonce}/three_d_secure/lookup"),
            data = request.build(cardinalConsumerSessionId))
        return fromJson(responseBody)
    }

    suspend fun authenticateCardinalJWT(
        threeDSecureParams: ThreeDSecureParams?,
        cardinalJWT: String?
    ): ThreeDSecureParams {
        if (threeDSecureParams == null || cardinalJWT == null) {
            throw BraintreeException("threeDSecureParams or jwt is null")
        }

        val lookupCardNonce = threeDSecureParams.threeDSecureNonce
        val lookupNonce = threeDSecureParams.threeDSecureNonce?.string

        val body = JSONObject()
        try {
            body.put("jwt", cardinalJWT)
            body.put("paymentMethodNonce", lookupNonce)
        } catch (ignored: JSONException) {
        }

        val url = versionedPath(
            "${ApiClient.PAYMENT_METHOD_ENDPOINT}/$lookupNonce/three_d_secure/authenticate_from_jwt"
        )
        val responseBody = braintreeClient.sendPOST(
            url = url,
            data = body.toString()
        )
        val result: ThreeDSecureParams = fromJson(responseBody)
        if (result.hasError()) {
            result.threeDSecureNonce = lookupCardNonce
        }
        return result
    }
}
