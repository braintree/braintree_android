package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessageOfferType
import junit.framework.TestCase.assertEquals
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalMessagingOfferTypeUnitTest {
    @Test
    fun `test pay later short term offer type returns raw value pay later short term`() {
        assertEquals(
            PayPalMessageOfferType.PAY_LATER_SHORT_TERM,
            PayPalMessagingOfferType.PAY_LATER_SHORT_TERM.internalValue
        )
    }

    @Test
    fun `test pay later long term offer type returns raw value pay later long term`() {
        assertEquals(
            PayPalMessageOfferType.PAY_LATER_LONG_TERM,
            PayPalMessagingOfferType.PAY_LATER_LONG_TERM.internalValue
        )
    }

    @Test
    fun `test pay later pay in one offer type returns raw value pay later pay in 1`() {
        assertEquals(
            PayPalMessageOfferType.PAY_LATER_PAY_IN_1,
            PayPalMessagingOfferType.PAY_LATER_PAY_IN_ONE.internalValue
        )
    }

    @Test
    fun `test PayPal credit no interest offer type returns raw value PayPal credit no interest`() {
        assertEquals(
            PayPalMessageOfferType.PAYPAL_CREDIT_NO_INTEREST,
            PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST.internalValue
        )
    }
}
