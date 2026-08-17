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
        assertEquals("STICKY_FI", input.getString("fetchPaymentMethodType"))
        assertEquals("pmid-jwt", input.getString("paymentMethodIdJwt"))
        assertEquals("BT_NATIVE_SDK", input.getString("integrationChannel"))
        assertFalse(input.has("orderId"))
    }

    @Test
    fun fromApprovedCheckout_buildsOrderInput() {
        val body = GetPayPalSavedPaymentMethodGraphQLBody.fromApprovedCheckout("order-123")
        val input = body.getJSONObject(GraphQLConstants.Keys.VARIABLES)
            .getJSONObject(GraphQLConstants.Keys.INPUT)

        assertEquals("FI_FROM_APPROVED_CHECKOUT", input.getString("fetchPaymentMethodType"))
        assertEquals("order-123", input.getString("orderId"))
        assertEquals("BT_NATIVE_SDK", input.getString("integrationChannel"))
        assertFalse(input.has("paymentMethodIdJwt"))
    }
}
