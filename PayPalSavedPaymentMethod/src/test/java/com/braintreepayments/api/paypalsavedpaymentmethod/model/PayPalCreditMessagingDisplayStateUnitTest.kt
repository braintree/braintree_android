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

    private fun result(mainText: String) = PayPalCreditMessagingResult(
        mainItems = listOf(
            PayPalCreditMessageItem(
                type = PayPalCreditMessageItemType.TEXT,
                text = mainText,
                alternativeText = null,
                clickUrl = null,
                sourceUrl = null,
                name = null,
                isEmbeddable = null
            )
        ),
        disclaimerItems = emptyList(),
        actionItems = emptyList(),
        messageId = null,
        messageType = null,
        impressionUrl = null
    )
}
