package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.ExperimentalBetaApi
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalMessagingRequestUnitTest {

    @Test
    fun `test PayPalMessagingRequest with empty init sets all values to default`() {
        val request = PayPalMessagingRequest()

        assertNull(request.amount)
        assertNull(request.pageType)
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
            pageType = PayPalMessagingPageType.HOME,
            offerType = PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST,
            buyerCountry = "US",
            logoType = PayPalMessagingLogoType.ALTERNATIVE,
            textAlignment = PayPalMessagingTextAlignment.LEFT,
            color = PayPalMessagingColor.WHITE
        )

        assertEquals(6.66, request.amount)
        assertEquals(PayPalMessagingPageType.HOME, request.pageType)
        assertEquals(PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST, request.offerType)
        assertEquals("US", request.buyerCountry)
        assertEquals(PayPalMessagingLogoType.ALTERNATIVE, request.logoType)
        assertEquals(PayPalMessagingTextAlignment.LEFT, request.textAlignment)
        assertEquals(PayPalMessagingColor.WHITE, request.color)
    }
}
