package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessageOfferType
import com.paypal.messages.config.message.style.PayPalMessageAlign
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingPlacementUnitTest {

    @Test
    fun testPlacement_withHome_returnsRawValueHome() {
        assertEquals("HOME", PayPalMessagingPlacement.HOME.rawValue)
    }

    @Test
    fun testPlacement_withCategory_returnsRawValueCategory() {
        assertEquals("CART", PayPalMessagingPlacement.CART.rawValue)
    }

    @Test
    fun testPlacement_withProduct_returnsRawValueProduct() {
        assertEquals("PRODUCT", PayPalMessagingPlacement.PRODUCT.rawValue)
    }

    @Test
    fun testPlacement_withPayment_returnsRawValuePayment() {
        assertEquals("PAYMENT", PayPalMessagingPlacement.PAYMENT.rawValue)
    }
}