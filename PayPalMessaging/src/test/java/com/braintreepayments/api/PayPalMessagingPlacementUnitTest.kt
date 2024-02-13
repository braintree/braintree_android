package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessageOfferType
import com.paypal.messages.config.message.style.PayPalMessageAlign
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingPlacementUnitTest {

    @Test
    fun testPlacement_withHome_returnsRawValueHome() {
        assertEquals(PayPalMessagingPlacement.HOME.rawValue, "HOME")
    }

    @Test
    fun testPlacement_withCategory_returnsRawValueCategory() {
        assertEquals(PayPalMessagingPlacement.CART.rawValue, "CART")
    }

    @Test
    fun testPlacement_withProduct_returnsRawValueProduct() {
        assertEquals(PayPalMessagingPlacement.PRODUCT.rawValue, "PRODUCT")
    }

    @Test
    fun testPlacement_withPayment_returnsRawValuePayment() {
        assertEquals(PayPalMessagingPlacement.PAYMENT.rawValue, "PAYMENT")
    }
}