package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayPalCreditMessagingContentUnitTest {

    @Test
    fun `toContent joins the main blocks and uses alternative text for images`() {
        val content = result(
            mainItems = listOf(
                text("4 interest-free payments of \$13.75 with "),
                image(alternativeText = "PayPal"),
                text(".")
            )
        ).toContent()

        assertEquals("4 interest-free payments of \$13.75 with PayPal.", content.message)
    }

    @Test
    fun `toContent appends the disclaimer after a space`() {
        val content = result(
            mainItems = listOf(text("Pay in 4 with PayPal.")),
            disclaimerItems = listOf(text("US only."))
        ).toContent()

        assertEquals("Pay in 4 with PayPal. US only.", content.message)
    }

    @Test
    fun `toContent trims the parts so a trailing space in the main text adds no double space`() {
        val content = result(
            mainItems = listOf(text("Pay in 4 with PayPal. ")),
            disclaimerItems = listOf(text("US only."))
        ).toContent()

        assertEquals("Pay in 4 with PayPal. US only.", content.message)
    }

    @Test
    fun `toContent returns an empty message when the main text is blank, even with a disclaimer`() {
        val content = result(mainItems = listOf(text("  ")), disclaimerItems = listOf(text("US only."))).toContent()

        assertEquals("", content.message)
    }

    @Test
    fun `toContent uses the first link action for learn more`() {
        val content = result(
            actionItems = listOf(
                text("Not a link"),
                link(text = "Learn more", clickUrl = "https://www.paypal.com/learn"),
                link(text = "Second link", clickUrl = "https://www.paypal.com/second")
            )
        ).toContent()

        assertEquals("Learn more", content.learnMoreText)
        assertEquals("https://www.paypal.com/learn", content.learnMoreUrl)
    }

    @Test
    fun `toContent skips a link without text or url`() {
        val content = result(
            actionItems = listOf(
                link(text = "  ", clickUrl = "https://www.paypal.com/blank"),
                link(text = "No url", clickUrl = null),
                link(text = "Learn more", clickUrl = "https://www.paypal.com/learn")
            )
        ).toContent()

        assertEquals("Learn more", content.learnMoreText)
        assertEquals("https://www.paypal.com/learn", content.learnMoreUrl)
    }

    @Test
    fun `toContent uses the link's alternative text when it has no text`() {
        val link = item(
            PayPalCreditMessageItemType.LINK,
            alternativeText = "Learn more about Pay in 4",
            clickUrl = "https://www.paypal.com/learn"
        )

        val content = result(actionItems = listOf(link)).toContent()

        assertEquals("Learn more about Pay in 4", content.learnMoreText)
        assertEquals("https://www.paypal.com/learn", content.learnMoreUrl)
    }

    @Test
    fun `toContent uses the link's alternative text when its text is blank`() {
        val link = item(
            PayPalCreditMessageItemType.LINK,
            text = "",
            alternativeText = "Learn more about Pay in 4",
            clickUrl = "https://www.paypal.com/learn"
        )

        val content = result(actionItems = listOf(link)).toContent()

        assertEquals("Learn more about Pay in 4", content.learnMoreText)
    }

    @Test
    fun `toContent prefers the link's text over its alternative text`() {
        val link = item(
            PayPalCreditMessageItemType.LINK,
            text = "Learn more",
            alternativeText = "Learn more about Pay in 4",
            clickUrl = "https://www.paypal.com/learn"
        )

        val content = result(actionItems = listOf(link)).toContent()

        assertEquals("Learn more", content.learnMoreText)
    }

    @Test
    fun `toContent carries the message id, message type and impression url for analytics`() {
        val content = result().copy(impressionUrl = "https://www.paypal.com/impression").toContent()

        assertEquals("message-id", content.messageId)
        assertEquals("PLST_SQ", content.messageType)
        assertEquals("https://www.paypal.com/impression", content.impressionUrl)
    }

    @Test
    fun `toContent leaves learn more null when there is no link`() {
        val content = result(actionItems = emptyList()).toContent()

        assertNull(content.learnMoreText)
        assertNull(content.learnMoreUrl)
    }

    private fun result(
        mainItems: List<PayPalCreditMessageItem> = listOf(text("Pay in 4 with PayPal.")),
        disclaimerItems: List<PayPalCreditMessageItem> = emptyList(),
        actionItems: List<PayPalCreditMessageItem> = emptyList()
    ) = PayPalCreditMessagingResult(
        mainItems = mainItems,
        disclaimerItems = disclaimerItems,
        actionItems = actionItems,
        messageId = "message-id",
        messageType = "PLST_SQ",
        impressionUrl = null
    )

    private fun text(text: String) = item(PayPalCreditMessageItemType.TEXT, text = text)

    private fun image(alternativeText: String) =
        item(PayPalCreditMessageItemType.IMAGE, text = "logo", alternativeText = alternativeText)

    private fun link(text: String, clickUrl: String?) =
        item(PayPalCreditMessageItemType.LINK, text = text, clickUrl = clickUrl)

    private fun item(
        type: PayPalCreditMessageItemType,
        text: String? = null,
        alternativeText: String? = null,
        clickUrl: String? = null
    ) = PayPalCreditMessageItem(
        type = type,
        text = text,
        alternativeText = alternativeText,
        clickUrl = clickUrl,
        sourceUrl = null,
        name = null,
        isEmbeddable = null
    )
}
