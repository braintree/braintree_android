package com.braintreepayments.api.uicomponents

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentButtonStyleUnitTest {

    @Test
    fun `toStyle maps every field of each PayPal color`() {
        listOf(PayPalButtonColor.Blue, PayPalButtonColor.Black, PayPalButtonColor.White).forEach { color ->
            val style = color.toStyle()

            assertEquals(color.logoId, style.logoId)
            assertEquals(color.spinnerId, style.spinnerId)
            assertEquals(color.default, style.default)
            assertEquals(color.hover, style.hover)
            assertEquals(color.focus, style.focus)
            assertEquals(color.focusHover, style.focusHover)
            assertEquals(color.pressed, style.pressed)
        }
    }

    @Test
    fun `Pay Later styles share PayPal colors and spinners but use their own logo`() {
        listOf(
            PayLaterButtonColor.Blue to PayPalButtonColor.Blue,
            PayLaterButtonColor.Black to PayPalButtonColor.Black,
            PayLaterButtonColor.White to PayPalButtonColor.White
        ).forEach { (payLater, payPal) ->
            val style = payLater.toStyle()

            assertEquals(payLater.logoId, style.logoId)
            assertEquals(payPal.spinnerId, style.spinnerId)
            assertEquals(payPal.default, style.default)
            assertEquals(payPal.pressed, style.pressed)
        }
    }
}
