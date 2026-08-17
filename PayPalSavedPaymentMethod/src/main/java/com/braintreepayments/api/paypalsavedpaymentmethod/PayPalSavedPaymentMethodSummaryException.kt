package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONObject

/**
 * Exception thrown when a saved payment method fetch fails.
 *
 * @property errorClass The GraphQL `extensions.errorClass` ("AUTHENTICATION" | "INTERNAL"), or null
 * for a client-side error such as a missing `paymentMethodIdJwt`.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodSummaryException internal constructor(
    val errorClass: String?,
    message: String?,
    cause: Throwable? = null,
) : BraintreeException(message, cause) {

    internal companion object {

        const val MISSING_PAYMENT_METHOD_ID_JWT =
            "paymentMethodIdJwt must not be blank to fetch the vaulted funding instrument."

        private const val ERROR_CLASS_KEY = "errorClass"

        /**
         * Builds an exception from a GraphQL response whose `errors[]` array is populated, reading
         * the first error's `message` and `extensions.errorClass`.
         */
        fun fromGraphQLResponse(response: JSONObject): PayPalSavedPaymentMethodSummaryException {
            val firstError = response
                .optJSONArray(GraphQLConstants.Keys.ERRORS)
                ?.optJSONObject(0)
            val errorClass = firstError
                ?.optJSONObject(GraphQLConstants.Keys.EXTENSIONS)
                ?.let { Json.optString(it, ERROR_CLASS_KEY, null) }
            val message = firstError
                ?.let { Json.optString(it, GraphQLConstants.Keys.MESSAGE, null) }
                ?: response.toString()
            return PayPalSavedPaymentMethodSummaryException(errorClass, message)
        }
    }
}
