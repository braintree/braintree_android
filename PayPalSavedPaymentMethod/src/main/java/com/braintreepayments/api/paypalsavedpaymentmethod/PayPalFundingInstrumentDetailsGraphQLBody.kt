package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONObject

/**
 * The POST body for the GraphQL query `PaypalFundingInstrumentDetails`. A null [paymentMethodIdJwt], [orderId] or
 * [merchantAccountId] is omitted from the request.
 *
 * @property fundingInstrumentType Which funding instrument to resolve.
 * @property paymentMethodIdJwt The client token's payment method ID JWT. Sent for
 * [PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT].
 * @property orderId The approved checkout order ID. Sent for
 * [PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT].
 * @property merchantAccountId Optional. A non-default merchant account to resolve the funding instrument against.
 */
internal data class PayPalFundingInstrumentDetailsGraphQLBody(
    val fundingInstrumentType: PayPalFundingInstrumentFetchType,
    val paymentMethodIdJwt: String?,
    val orderId: String?,
    val merchantAccountId: String?
) {

    /**
     * @return The JSON body for [com.braintreepayments.api.core.BraintreeClient.sendGraphQLPOST].
     */
    fun toJson(): JSONObject {
        val input = JSONObject()
            .put(FUNDING_INSTRUMENT_TYPE_KEY, fundingInstrumentType.rawValue)
            .put(INTEGRATION_CHANNEL_KEY, INTEGRATION_CHANNEL)
            .put(OS_TYPE_KEY, OS_TYPE)
            .putOpt(PAYMENT_METHOD_ID_JWT_KEY, paymentMethodIdJwt)
            .putOpt(ORDER_ID_KEY, orderId)
            .putOpt(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId)

        return JSONObject()
            .put(GraphQLConstants.Keys.QUERY, QUERY)
            .put(GraphQLConstants.Keys.VARIABLES, JSONObject().put(GraphQLConstants.Keys.INPUT, input))
    }

    private companion object {
        const val FUNDING_INSTRUMENT_TYPE_KEY = "fundingInstrumentType"
        const val INTEGRATION_CHANNEL_KEY = "integrationChannel"
        const val OS_TYPE_KEY = "osType"
        const val PAYMENT_METHOD_ID_JWT_KEY = "paymentMethodIdJwt"
        const val ORDER_ID_KEY = "orderId"
        const val MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId"

        const val INTEGRATION_CHANNEL = "BT_NATIVE_SDK"
        const val OS_TYPE = "ANDROID"

        const val QUERY =
            "query PaypalFundingInstrumentDetails(\$input: PayPalFundingInstrumentDetailsInput!) {" +
                "  paypalFundingInstrumentDetails(input: \$input) {" +
                "    payer {" +
                "      email" +
                "      editable" +
                "    }" +
                "    paymentMethods {" +
                "      label" +
                "      imageUrl" +
                "      lastDigits" +
                "      type" +
                "      subtype" +
                "    }" +
                "  }" +
                "}"
    }
}
