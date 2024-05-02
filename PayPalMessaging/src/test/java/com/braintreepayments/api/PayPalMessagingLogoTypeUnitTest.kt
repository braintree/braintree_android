package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageLogoType
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingLogoTypeUnitTest {

    @Test
    fun `test inline logo returns raw value inline`() {
        assertEquals(PayPalMessageLogoType.INLINE, PayPalMessagingLogoType.INLINE.internalValue)
    }

    @Test
    fun `test primary logo returns raw value primary`() {
        assertEquals(PayPalMessageLogoType.PRIMARY, PayPalMessagingLogoType.PRIMARY.internalValue)
    }

    @Test
    fun `test alternative logo returns raw value alternative`() {
        assertEquals(PayPalMessageLogoType.ALTERNATIVE, PayPalMessagingLogoType.ALTERNATIVE.internalValue)
    }

    @Test
    fun `test simple logo returns raw value none`() {
        assertEquals(PayPalMessageLogoType.NONE, PayPalMessagingLogoType.NONE.internalValue)
    }
}
