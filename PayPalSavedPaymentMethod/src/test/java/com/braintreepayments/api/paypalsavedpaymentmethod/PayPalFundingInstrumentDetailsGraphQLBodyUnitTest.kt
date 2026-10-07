package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalFundingInstrumentDetailsGraphQLBodyUnitTest {

    @Test
    fun `toJson for a buyer default billing agreement sends STICKY_FI and the paymentMethodIdJwt`() {
        val input = input(
            PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
            paymentMethodIdJwt = "jwt-123"
        )

        assertEquals("STICKY_FI", input.getString("fundingInstrumentType"))
        assertEquals("jwt-123", input.getString("paymentMethodIdJwt"))
        assertFalse(input.has("orderId"))
    }

    @Test
    fun `toJson for a buyer updated billing agreement sends FI_FROM_APPROVED_CHECKOUT and the orderId`() {
        val input = input(PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT, orderId = "order-456")

        assertEquals("FI_FROM_APPROVED_CHECKOUT", input.getString("fundingInstrumentType"))
        assertEquals("order-456", input.getString("orderId"))
        assertFalse(input.has("paymentMethodIdJwt"))
    }

    @Test
    fun `toJson always sends the integrationChannel and osType`() {
        val input = input(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)

        assertEquals("BT_NATIVE_SDK", input.getString("integrationChannel"))
        assertEquals("ANDROID", input.getString("osType"))
    }

    @Test
    fun `toJson includes merchantAccountId when provided`() {
        val input = input(
            PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
            merchantAccountId = "merchant-account-1"
        )

        assertEquals("merchant-account-1", input.getString("merchantAccountId"))
    }

    @Test
    fun `toJson omits merchantAccountId when null`() {
        val input = input(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)

        assertFalse(input.has("merchantAccountId"))
    }

    @Test
    fun `toJson sends the PaypalFundingInstrumentDetails query`() {
        val query = PayPalFundingInstrumentDetailsGraphQLBody(
            fundingInstrumentType = PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
            paymentMethodIdJwt = null,
            orderId = null,
            merchantAccountId = null
        ).toJson().getString(GraphQLConstants.Keys.QUERY)

        assertEquals(
            "query PaypalFundingInstrumentDetails(\$input: PayPalFundingInstrumentDetailsInput!){" +
                "paypalFundingInstrumentDetails(input: \$input){" +
                "payer{email editable}" +
                "paymentMethods{label imageUrl lastDigits type subtype}" +
                "}}",
            query.normalized()
        )
    }

    private fun input(
        fundingInstrumentType: PayPalFundingInstrumentFetchType,
        paymentMethodIdJwt: String? = null,
        orderId: String? = null,
        merchantAccountId: String? = null
    ): JSONObject = PayPalFundingInstrumentDetailsGraphQLBody(
        fundingInstrumentType = fundingInstrumentType,
        paymentMethodIdJwt = paymentMethodIdJwt,
        orderId = orderId,
        merchantAccountId = merchantAccountId
    ).toJson().getJSONObject(GraphQLConstants.Keys.VARIABLES).getJSONObject(GraphQLConstants.Keys.INPUT)

    // Collapses whitespace and drops it around braces, so the check ignores layout but not field names.
    private fun String.normalized(): String =
        replace(Regex("\\s+"), " ").replace(Regex(" ?([{}]) ?"), "$1").trim()
}
