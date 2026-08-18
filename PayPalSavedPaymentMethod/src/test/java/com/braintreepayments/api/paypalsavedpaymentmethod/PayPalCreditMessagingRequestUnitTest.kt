package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalCreditMessagingRequestUnitTest {

    @Test
    fun build_buildsExpectedBody() {
        val request = PayPalCreditMessagingRequest(
            flowContext = FlowContext(),
            messagePlacements = listOf(
                MessagePlacement(amount = Amount(currencyCode = "USD", value = "55.00"))
            )
        )

        val json = request.build()

        val flowContext = json.getJSONObject("flow_context")
        assertEquals("MOBILE_APP", flowContext.getString("channel"))
        assertEquals("EARLY_PRESENTMENT", flowContext.getString("flow_specifier"))
        assertTrue(flowContext.getJSONArray("attributes").toString().contains("BRAND_BRAINTREE"))
        assertTrue(flowContext.getJSONArray("attributes").toString().contains("EXPERIENCE_ANDROID_SDK"))
        assertTrue(flowContext.getJSONArray("attributes").toString().contains("EXPERIENCE_VIEW_EDIT_FI"))

        val placement = json.getJSONArray("message_placements").getJSONObject(0)
        val amount = placement.getJSONObject("amount")
        assertEquals("USD", amount.getString("currency_code"))
        assertEquals("55.00", amount.getString("value"))
        val contentAttributes = placement.getJSONArray("content_attributes").toString()
        assertTrue(contentAttributes.contains("ALTERNATIVE_PREFIX_UPPERCASE_OR"))
        assertTrue(contentAttributes.contains("MESSAGE_LENGTH_COMPACT"))
    }

    @Test
    fun build_withCustomFlowContextAndContentAttributes_overridesDefaults() {
        val request = PayPalCreditMessagingRequest(
            flowContext = FlowContext(attributes = listOf("BRAND_BRAINTREE", "EXPERIENCE_IOS_SDK")),
            messagePlacements = listOf(
                MessagePlacement(
                    amount = Amount(currencyCode = "GBP", value = "100.00"),
                    contentAttributes = listOf("CUSTOM_ATTRIBUTE")
                )
            )
        )

        val json = request.build()

        val flowContext = json.getJSONObject("flow_context")
        assertTrue(flowContext.getJSONArray("attributes").toString().contains("EXPERIENCE_IOS_SDK"))

        val placement = json.getJSONArray("message_placements").getJSONObject(0)
        assertEquals("GBP", placement.getJSONObject("amount").getString("currency_code"))
        assertTrue(placement.getJSONArray("content_attributes").toString().contains("CUSTOM_ATTRIBUTE"))
    }
}
