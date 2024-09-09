package com.braintreepayments.api.paypalmessaging

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.paypal.messages.config.message.style.PayPalMessageAlignment
import junit.framework.TestCase.assertEquals
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalMessagingTextAlignmentUnitTest {

    @Test
    fun `test left text alignment returns raw value left align`() {
        assertEquals(PayPalMessageAlignment.LEFT, PayPalMessagingTextAlignment.LEFT.internalValue)
    }

    @Test
    fun `test right text alignment returns raw value right align`() {
        assertEquals(PayPalMessageAlignment.RIGHT, PayPalMessagingTextAlignment.RIGHT.internalValue)
    }

    @Test
    fun `test center text alignment returns raw value center align`() {
        assertEquals(PayPalMessageAlignment.CENTER, PayPalMessagingTextAlignment.CENTER.internalValue)
    }
}
