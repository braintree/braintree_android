package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.GraphQLConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPayPalSavedPaymentMethodGraphQLBodyUnitTest {

    @Test
    fun stickyFi_buildsStickyFiInput() {
        val body = GetPayPalSavedPaymentMethodGraphQLBody.stickyFi("pmid-jwt")
        val input = body.getJSONObject(GraphQLConstants.Keys.VARIABLES)
            .getJSONObject(GraphQLConstants.Keys.INPUT)

        assertTrue(body.getString(GraphQLConstants.Keys.QUERY).contains("PaypalFundingInstrumentDetails"))
        assertEquals("STICKY_FI", input.getString("fundingInstrumentType"))
        assertEquals("pmid-jwt", input.getString("paymentMethodIdJwt"))
        assertEquals("BT_NATIVE_SDK", input.getString("integrationChannel"))
        assertFalse(input.has("orderId"))
        assertFalse(input.has("merchantAccountId"))
    }

    @Test
    fun stickyFi_withMerchantAccountId_includesIt() {
        val body = GetPayPalSavedPaymentMethodGraphQLBody.stickyFi("pmid-jwt", "merchant-account-1")
        val input = body.getJSONObject(GraphQLConstants.Keys.VARIABLES)
            .getJSONObject(GraphQLConstants.Keys.INPUT)

        assertEquals("merchant-account-1", input.getString("merchantAccountId"))
    }

    @Test
    fun fromApprovedCheckout_buildsOrderInput() {
        val body = GetPayPalSavedPaymentMethodGraphQLBody.fromApprovedCheckout("order-123")
        val input = body.getJSONObject(GraphQLConstants.Keys.VARIABLES)
            .getJSONObject(GraphQLConstants.Keys.INPUT)

        assertEquals("FI_FROM_APPROVED_CHECKOUT", input.getString("fundingInstrumentType"))
        assertEquals("order-123", input.getString("orderId"))
        assertEquals("BT_NATIVE_SDK", input.getString("integrationChannel"))
        assertFalse(input.has("paymentMethodIdJwt"))
        assertFalse(input.has("merchantAccountId"))
    }

    @Test
    fun fromApprovedCheckout_withMerchantAccountId_includesIt() {
        val body = GetPayPalSavedPaymentMethodGraphQLBody.fromApprovedCheckout("order-123", "merchant-account-1")
        val input = body.getJSONObject(GraphQLConstants.Keys.VARIABLES)
            .getJSONObject(GraphQLConstants.Keys.INPUT)

        assertEquals("merchant-account-1", input.getString("merchantAccountId"))
    }
}
