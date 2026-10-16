package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalCreditMessagingDisplayStateUnitTest {

    @Test
    fun `a message with text maps to Content`() {
        val result = result(mainText = "Pay in 4 with PayPal.")

        assertEquals(
            PayPalCreditMessagingDisplayState.Content(
                PayPalCreditMessagingContent(
                    message = "Pay in 4 with PayPal.",
                    learnMoreText = null,
                    learnMoreUrl = null
                )
            ),
            result.toDisplayState()
        )
    }

    @Test
    fun `a null result maps to Hidden`() {
        val result: PayPalCreditMessagingResult? = null

        assertEquals(PayPalCreditMessagingDisplayState.Hidden, result.toDisplayState())
    }

    @Test
    fun `a blank message maps to Hidden`() {
        assertEquals(PayPalCreditMessagingDisplayState.Hidden, result(mainText = "   ").toDisplayState())
    }

    @Test
    fun `a disclaimer without main text maps to Hidden`() {
        assertEquals(
            PayPalCreditMessagingDisplayState.Hidden,
            result(mainText = "", disclaimerText = "US only.").toDisplayState()
        )
    }

    private fun result(mainText: String, disclaimerText: String? = null) = PayPalCreditMessagingResult(
        mainItems = listOf(textItem(mainText)),
        disclaimerItems = listOfNotNull(disclaimerText?.let(::textItem)),
        actionItems = emptyList(),
        messageId = null,
        messageType = null,
        impressionUrl = null
    )

    private fun textItem(text: String) = PayPalCreditMessageItem(
        type = PayPalCreditMessageItemType.TEXT,
        text = text,
        alternativeText = null,
        clickUrl = null,
        sourceUrl = null,
        name = null,
        isEmbeddable = null
    )
}
