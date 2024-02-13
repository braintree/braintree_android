package com.braintreepayments.api

import com.paypal.messages.config.PayPalMessageOfferType
import junit.framework.TestCase.assertEquals
import org.junit.Test
class PayPalMessagingOfferTypeUnitTest {
    @Test
    fun testOfferType_withPayLaterShortTerm_returnsRawValuePayLaterShortTerm() {
        assertEquals(PayPalMessagingOfferType.PAY_LATER_SHORT_TERM.offerTypeRawValue, PayPalMessageOfferType.PAY_LATER_SHORT_TERM)
    }

    @Test
    fun testOfferType_withPayLaterLongTerm_returnsRawValuePayLaterLongTerm() {
        assertEquals(PayPalMessagingOfferType.PAY_LATER_LONG_TERM.offerTypeRawValue, PayPalMessageOfferType.PAY_LATER_LONG_TERM)
    }

    @Test
    fun testOfferType_withPayLaterPayInOne_returnsRawValuePayLaterPayIn1() {
        assertEquals(PayPalMessagingOfferType.PAY_LATER_PAY_IN_ONE.offerTypeRawValue, PayPalMessageOfferType.PAY_LATER_PAY_IN_1)
    }

    @Test
    fun testOfferType_withPayPalCreditNoInterest_returnsRawValuePayPalCreditNoInterest() {
        assertEquals(PayPalMessagingOfferType.PAYPAL_CREDIT_NO_INTEREST.offerTypeRawValue, PayPalMessageOfferType.PAYPAL_CREDIT_NO_INTEREST)
    }
}