package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONObject

/**
 * Builds the request body for the shared `getSavedPaymentMethod` GraphQL operation. One query, two
 * callers — only the `fetchPaymentMethodType` discriminator and the identity field differ.
 */
internal object GetSavedPaypalPaymentMethodGraphQLBody {

    /**
     * Initial / sticky FI fetch, keyed by the `paymentMethodIdJwt` from the client token.
     */
    fun stickyFi(paymentMethodIdJwt: String): JSONObject = build(
        JSONObject().apply {
            put(FETCH_PAYMENT_METHOD_TYPE_KEY, STICKY_FI)
            put(PAYMENT_METHOD_ID_JWT_KEY, paymentMethodIdJwt)
            put(INTEGRATION_CHANNEL_KEY, BT_NATIVE_SDK)
        }
    )

    /**
     * Post-edit refresh, keyed by the approved-checkout `orderId`.
     */
    fun fromApprovedCheckout(orderId: String): JSONObject = build(
        JSONObject().apply {
            put(FETCH_PAYMENT_METHOD_TYPE_KEY, FI_FROM_APPROVED_CHECKOUT)
            put(ORDER_ID_KEY, orderId)
            put(INTEGRATION_CHANNEL_KEY, BT_NATIVE_SDK)
        }
    )

    private fun build(input: JSONObject): JSONObject = JSONObject().apply {
        put(GraphQLConstants.Keys.QUERY, QUERY)
        put(GraphQLConstants.Keys.VARIABLES, JSONObject().put(GraphQLConstants.Keys.INPUT, input))
    }

    private const val FETCH_PAYMENT_METHOD_TYPE_KEY = "fetchPaymentMethodType"
    private const val PAYMENT_METHOD_ID_JWT_KEY = "paymentMethodIdJwt"
    private const val ORDER_ID_KEY = "orderId"
    private const val INTEGRATION_CHANNEL_KEY = "integrationChannel"

    private const val STICKY_FI = "STICKY_FI"
    private const val FI_FROM_APPROVED_CHECKOUT = "FI_FROM_APPROVED_CHECKOUT"
    private const val BT_NATIVE_SDK = "BT_NATIVE_SDK"

    private const val QUERY =
        "query GetSavedPaymentMethod(\$input: getSavedPaymentMethodInput!) { " +
            "getSavedPaymentMethod(input: \$input) { " +
            "payer { email isEditable } " +
            "paymentMethods { label imageUrl lastDigits type subtype } " +
            "} }"
}
