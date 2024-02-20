package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessageOfferType
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingOfferTypeUnitTest {
    @Test
    fun testOfferType_withPayLaterShortTerm_returnsRawValuePayLaterShortTerm() {
        assertEquals(PayPalMessageOfferType.PAY_LATER_SHORT_TERM, PayPalMessagingOfferType.PAY_LATER_SHORT_TERM.offerTypeRawValue)
    }

    @Test
    fun testOfferType_withPayLaterLongTerm_returnsRawValuePayLaterLongTerm() {
        assertEquals(PayPalMessageOfferType.PAY_LATER_LONG_TERM, PayPalMessagingOfferType.PAY_LATER_LONG_TERM.offerTypeRawValue)
    }

    @Test
    fun testOfferType_withPayLaterPayInOne_returnsRawValuePayLaterPayIn1() {
        assertEquals(PayPalMessageOfferType.PAY_LATER_PAY_IN_1, PayPalMessagingOfferType.PAY_LATER_PAY_IN_ONE.offerTypeRawValue)
    }

    @Test
    fun testOfferType_withPayPalCreditNoInterest_returnsRawValuePayPalCreditNoInterest() {
        assertEquals(PayPalMessageOfferType.PAYPAL_CREDIT_NO_INTEREST, PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST.offerTypeRawValue)
    }
}
