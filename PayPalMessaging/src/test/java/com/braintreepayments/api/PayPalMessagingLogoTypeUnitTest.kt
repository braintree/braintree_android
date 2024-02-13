package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageLogoType
import junit.framework.TestCase.assertEquals
import org.junit.Test
class PayPalMessagingLogoTypeUnitTest {
    @Test
    fun testLogoType_withInline_returnsRawValueInline() {
        assertEquals(PayPalMessagingLogoType.INLINE.logoTypeRawValue, PayPalMessageLogoType.INLINE)
    }

    @Test
    fun testLogoType_withPrimary_returnsRawValuePrimary() {
        assertEquals(PayPalMessagingLogoType.PRIMARY.logoTypeRawValue, PayPalMessageLogoType.PRIMARY)
    }

    @Test
    fun testLogoType_withAlternative_returnsRawValueAlternative() {
        assertEquals(PayPalMessagingLogoType.ALTERNATIVE.logoTypeRawValue, PayPalMessageLogoType.ALTERNATIVE)
    }

    @Test
    fun testLogoType_withNone_returnsRawValueNone() {
        assertEquals(PayPalMessagingLogoType.SIMPLE.logoTypeRawValue, PayPalMessageLogoType.NONE)
    }
}
