package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONObject

/**
 * Builds the request body for the shared `paypalFundingInstrumentDetails` GraphQL operation. One
 * query, two callers — only the `fundingInstrumentType` discriminator and the identity field
 * differ.
 */
internal object GetPayPalSavedPaymentMethodGraphQLBody {

    /**
     * Initial / sticky FI fetch, keyed by the `paymentMethodIdJwt` from the client token.
     *
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     */
    fun stickyFi(paymentMethodIdJwt: String, merchantAccountId: String? = null): JSONObject = build(
        JSONObject().apply {
            put(FUNDING_INSTRUMENT_TYPE_KEY, STICKY_FI)
            put(PAYMENT_METHOD_ID_JWT_KEY, paymentMethodIdJwt)
            put(INTEGRATION_CHANNEL_KEY, BT_NATIVE_SDK)
            merchantAccountId?.let { put(MERCHANT_ACCOUNT_ID_KEY, it) }
        }
    )

    /**
     * Post-edit refresh, keyed by the approved-checkout `orderId`.
     *
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     */
    fun fromApprovedCheckout(orderId: String, merchantAccountId: String? = null): JSONObject = build(
        JSONObject().apply {
            put(FUNDING_INSTRUMENT_TYPE_KEY, FI_FROM_APPROVED_CHECKOUT)
            put(ORDER_ID_KEY, orderId)
            put(INTEGRATION_CHANNEL_KEY, BT_NATIVE_SDK)
            merchantAccountId?.let { put(MERCHANT_ACCOUNT_ID_KEY, it) }
        }
    )

    private fun build(input: JSONObject): JSONObject = JSONObject().apply {
        put(GraphQLConstants.Keys.QUERY, QUERY)
        put(GraphQLConstants.Keys.VARIABLES, JSONObject().put(GraphQLConstants.Keys.INPUT, input))
    }

    private const val FUNDING_INSTRUMENT_TYPE_KEY = "fundingInstrumentType"
    private const val PAYMENT_METHOD_ID_JWT_KEY = "paymentMethodIdJwt"
    private const val ORDER_ID_KEY = "orderId"
    private const val INTEGRATION_CHANNEL_KEY = "integrationChannel"
    private const val MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId"

    private const val STICKY_FI = "STICKY_FI"
    private const val FI_FROM_APPROVED_CHECKOUT = "FI_FROM_APPROVED_CHECKOUT"
    private const val BT_NATIVE_SDK = "BT_NATIVE_SDK"

    private const val QUERY =
        "query PaypalFundingInstrumentDetails(\$input: PayPalFundingInstrumentDetailsInput!) { " +
            "paypalFundingInstrumentDetails(input: \$input) { " +
            "payer { email editable } " +
            "paymentMethods { label imageUrl lastDigits type subtype } " +
            "} }"
}
