package com.braintreepayments.api

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingPlacementUnitTest {

    @Test
    fun `test home placement returns string value home`() {
        assertEquals("HOME", PayPalMessagingPlacement.HOME.rawValue)
    }

    @Test
    fun `test category placement returns string value category`() {
        assertEquals("CATEGORY", PayPalMessagingPlacement.CATEGORY.rawValue)
    }

    @Test
    fun `test product placement returns string value product`() {
        assertEquals("PRODUCT", PayPalMessagingPlacement.PRODUCT.rawValue)
    }

    @Test
    fun `test cart placement returns string value cart`() {
        assertEquals("CART", PayPalMessagingPlacement.CART.rawValue)
    }

    @Test
    fun `test payment placement returns string value payment`() {
        assertEquals("PAYMENT", PayPalMessagingPlacement.PAYMENT.rawValue)
    }
}
