package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.ExperimentalBetaApi
import com.paypal.messages.config.PayPalMessagePageType
import junit.framework.TestCase.assertEquals
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalMessagingPageTypeUnitTest {

    @Test
    fun `test home placement returns message type value home`() {
        assertEquals(PayPalMessagePageType.HOME, PayPalMessagingPageType.HOME.internalValue)
    }

    @Test
    fun `test category placement returns message type value product details`() {
        assertEquals(PayPalMessagePageType.PRODUCT_DETAILS, PayPalMessagingPageType.PRODUCT_DETAILS.internalValue)
    }

    @Test
    fun `test product placement returns message type value product listing`() {
        assertEquals(PayPalMessagePageType.PRODUCT_LISTING, PayPalMessagingPageType.PRODUCT_LISTING.internalValue)
    }

    @Test
    fun `test cart placement returns message type value cart`() {
        assertEquals(PayPalMessagePageType.CART, PayPalMessagingPageType.CART.internalValue)
    }

    @Test
    fun `test cart placement returns message type value mini cart`() {
        assertEquals(PayPalMessagePageType.MINI_CART, PayPalMessagingPageType.MINI_CART.internalValue)
    }

    @Test
    fun `test payment placement returns message type value checkout`() {
        assertEquals(PayPalMessagePageType.CHECKOUT, PayPalMessagingPageType.CHECKOUT.internalValue)
    }

    @Test
    fun `test payment placement returns message type value search results`() {
        assertEquals(PayPalMessagePageType.SEARCH_RESULTS, PayPalMessagingPageType.SEARCH_RESULTS.internalValue)
    }
}
