package com.braintreepayments.api

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class PayPalMessagingRequestUnitTest {

    @Test
    fun testPayPalMessagingRequest_withEmptyInit_setsAllValuesToDefault() {
        var request = PayPalMessagingRequest()

        assertNull(request.amount)
        assertNull(request.placement)
        assertNull(request.offerType)
        assertNull(request.buyerCountry)
        assertEquals(request.logoType, PayPalMessagingLogoType.INLINE)
        assertEquals(request.textAlignment, PayPalMessagingTextAlignment.RIGHT)
        assertEquals(request.color, PayPalMessagingColor.BLACK)
    }

    @Test
    fun testPayPalMessagingRequest_withAllValuesInitialized_setsAllValues() {
        var request = PayPalMessagingRequest(
            amount = 6.66,
            placement = PayPalMessagingPlacement.HOME,
            PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST,
            "US",
            PayPalMessagingLogoType.ALTERNATIVE,
            PayPalMessagingTextAlignment.LEFT,
            PayPalMessagingColor.WHITE
        )

        assertEquals(request.amount, 6.66)
        assertEquals(request.placement, PayPalMessagingPlacement.HOME,)
        assertEquals(request.offerType, PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST)
        assertEquals(request.buyerCountry, "US")
        assertEquals(request.logoType, PayPalMessagingLogoType.ALTERNATIVE)
        assertEquals(request.textAlignment, PayPalMessagingTextAlignment.LEFT)
        assertEquals(request.color, PayPalMessagingColor.WHITE)
    }
}

