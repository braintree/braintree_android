package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessagePageType
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingPageTypeUnitTest {

    @Test
    fun `test home placement returns message type value home`() {
        assertEquals(PayPalMessagePageType.HOME, PayPalMessagingPageType.HOME.rawValue)
    }

    @Test
    fun `test category placement returns message type value product details`() {
        assertEquals(PayPalMessagePageType.PRODUCT_DETAILS, PayPalMessagingPageType.PRODUCT_DETAILS.rawValue)
    }

    @Test
    fun `test product placement returns message type value product listing`() {
        assertEquals(PayPalMessagePageType.PRODUCT_LISTING, PayPalMessagingPageType.PRODUCT_LISTING.rawValue)
    }

    @Test
    fun `test cart placement returns message type value cart`() {
        assertEquals(PayPalMessagePageType.CART, PayPalMessagingPageType.CART.rawValue)
    }

    @Test
    fun `test cart placement returns message type value mini cart`() {
        assertEquals(PayPalMessagePageType.MINI_CART, PayPalMessagingPageType.MINI_CART.rawValue)
    }

    @Test
    fun `test payment placement returns message type value checkout`() {
        assertEquals(PayPalMessagePageType.CHECKOUT, PayPalMessagingPageType.CHECKOUT.rawValue)
    }

    @Test
    fun `test payment placement returns message type value search results`() {
        assertEquals(PayPalMessagePageType.SEARCH_RESULTS, PayPalMessagingPageType.SEARCH_RESULTS.rawValue)
    }
}
