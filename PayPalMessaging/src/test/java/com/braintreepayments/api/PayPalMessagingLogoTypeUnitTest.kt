package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageLogoType
import junit.framework.TestCase.assertEquals
import org.junit.Test
class PayPalMessagingLogoTypeUnitTest {

    @Test
    fun testLogoType_withInline_returnsRawValueInline() {
        assertEquals(PayPalMessageLogoType.INLINE, PayPalMessagingLogoType.INLINE.logoTypeRawValue)
    }

    @Test
    fun testLogoType_withPrimary_returnsRawValuePrimary() {
        assertEquals(PayPalMessageLogoType.PRIMARY, PayPalMessagingLogoType.PRIMARY.logoTypeRawValue)
    }

    @Test
    fun testLogoType_withAlternative_returnsRawValueAlternative() {
        assertEquals(PayPalMessageLogoType.ALTERNATIVE, PayPalMessagingLogoType.ALTERNATIVE.logoTypeRawValue)
    }

    @Test
    fun testLogoType_withNone_returnsRawValueNone() {
        assertEquals(PayPalMessageLogoType.NONE, PayPalMessagingLogoType.SIMPLE.logoTypeRawValue)
    }
}
