package com.braintreepayments.api.paypalsavedpaymentmethod

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalCreditMessagingRequestUnitTest {

    private val body = PayPalCreditMessagingRequest(amount = "55.00", currencyCode = "USD").toJson()

    @Test
    fun `toJson sends the amount and currencyCode under message_placements`() {
        val amount = body.getJSONArray("message_placements").getJSONObject(0).getJSONObject("amount")

        assertEquals("55.00", amount.getString("value"))
        assertEquals("USD", amount.getString("currency_code"))
    }

    @Test
    fun `toJson sends the fixed content_attributes`() {
        val contentAttributes = body.getJSONArray("message_placements")
            .getJSONObject(0)
            .getJSONArray("content_attributes")

        assertEquals(2, contentAttributes.length())
        assertEquals("ALTERNATIVE_PREFIX_UPPERCASE_OR", contentAttributes.getString(0))
        assertEquals("MESSAGE_LENGTH_COMPACT", contentAttributes.getString(1))
    }

    @Test
    fun `toJson sends the fixed BT native flow_context`() {
        val flowContext = body.getJSONObject("flow_context")
        val attributes = flowContext.getJSONArray("attributes")

        assertEquals("MOBILE_APP", flowContext.getString("channel"))
        assertEquals("EARLY_PRESENTMENT", flowContext.getString("flow_specifier"))
        assertEquals(4, attributes.length())
        assertEquals("BRAND_BRAINTREE", attributes.getString(0))
        assertEquals("EXPERIENCE_ANDROID_SDK", attributes.getString(1))
        assertEquals("EXPERIENCE_VIEW_EDIT_FI", attributes.getString(2))
        assertEquals("EXPERIENCE_EXTERNAL_DIRECT", attributes.getString(3))
    }
}
