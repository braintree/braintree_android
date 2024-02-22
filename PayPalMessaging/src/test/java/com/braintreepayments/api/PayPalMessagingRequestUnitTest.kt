package com.braintreepayments.api

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class PayPalMessagingRequestUnitTest {

    @Test
    fun `test PayPalMessagingRequest with empty init sets all values to default`() {
        val request = PayPalMessagingRequest()

        assertNull(request.amount)
        assertNull(request.placement)
        assertNull(request.offerType)
        assertNull(request.buyerCountry)
        assertEquals(PayPalMessagingLogoType.INLINE, request.logoType)
        assertEquals(PayPalMessagingTextAlignment.RIGHT, request.textAlignment)
        assertEquals(PayPalMessagingColor.BLACK, request.color)
    }

    @Test
    fun `test PayPalMessagingRequest with all values initialized sets all values`() {
        val request = PayPalMessagingRequest(
            amount = 6.66,
            placement = PayPalMessagingPlacement.HOME,
            offerType = PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST,
            buyerCountry = "US",
            logoType = PayPalMessagingLogoType.ALTERNATIVE,
            textAlignment = PayPalMessagingTextAlignment.LEFT,
            color = PayPalMessagingColor.WHITE
        )

        assertEquals(6.66, request.amount)
        assertEquals(PayPalMessagingPlacement.HOME, request.placement)
        assertEquals(PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST, request.offerType)
        assertEquals("US", request.buyerCountry)
        assertEquals(PayPalMessagingLogoType.ALTERNATIVE, request.logoType)
        assertEquals(PayPalMessagingTextAlignment.LEFT, request.textAlignment)
        assertEquals(PayPalMessagingColor.WHITE, request.color)
    }
}
