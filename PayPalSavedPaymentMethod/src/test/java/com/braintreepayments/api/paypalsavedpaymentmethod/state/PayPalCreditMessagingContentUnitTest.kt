package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessageItem
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessageItemType
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
    fun `toContent uses the first action item with text and a url for learn more`() {
        val content = result(
            actionItems = listOf(
                text("Not a link"),
                link(text = "Learn more", clickUrl = "https://www.paypal.com/learn"),
                link(text = "Second link", clickUrl = "https://www.paypal.com/second")
            )
        ).toContent()

        assertEquals("Learn more", content.learnMore?.text)
        assertEquals("https://www.paypal.com/learn", content.learnMore?.url)
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

        assertEquals("Learn more", content.learnMore?.text)
        assertEquals("https://www.paypal.com/learn", content.learnMore?.url)
    }

    @Test
    fun `toContent skips a link whose url is not https`() {
        val content = result(
            actionItems = listOf(
                link(text = "Insecure", clickUrl = "http://www.paypal.com/insecure"),
                link(text = "Script", clickUrl = "javascript:alert(1)"),
                link(text = "Learn more", clickUrl = "https://www.paypal.com/learn")
            )
        ).toContent()

        assertEquals("Learn more", content.learnMore?.text)
        assertEquals("https://www.paypal.com/learn", content.learnMore?.url)
    }

    @Test
    fun `toContent drops an impression url that is not https`() {
        val content = result().copy(impressionUrl = "http://www.paypal.com/impression").toContent()

        assertNull(content.impressionUrl)
    }

    @Test
    fun `toContent uses the link's alternative text when it has no text`() {
        val link = item(
            PayPalCreditMessageItemType.LINK,
            alternativeText = "Learn more about Pay in 4",
            clickUrl = "https://www.paypal.com/learn"
        )

        val content = result(actionItems = listOf(link)).toContent()

        assertEquals("Learn more about Pay in 4", content.learnMore?.text)
        assertEquals("https://www.paypal.com/learn", content.learnMore?.url)
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

        assertEquals("Learn more about Pay in 4", content.learnMore?.text)
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

        assertEquals("Learn more", content.learnMore?.text)
    }

    @Test
    fun `toContent uses an action item of any type that has text and a url`() {
        val action =
            item(PayPalCreditMessageItemType.TEXT, text = "Learn more", clickUrl = "https://www.paypal.com/learn")

        val content = result(actionItems = listOf(action)).toContent()

        assertEquals("Learn more", content.learnMore?.text)
        assertEquals("https://www.paypal.com/learn", content.learnMore?.url)
    }

    @Test
    fun `toContent carries whether the link may open in the app`() {
        val embeddable = item(
            PayPalCreditMessageItemType.LINK,
            text = "Learn more",
            clickUrl = "https://www.paypal.com/learn",
            isEmbeddable = true
        )

        assertEquals(true, result(actionItems = listOf(embeddable)).toContent().learnMore?.isEmbeddable)
        assertEquals(
            false,
            result(actionItems = listOf(embeddable.copy(isEmbeddable = null))).toContent().learnMore?.isEmbeddable
        )
    }

    @Test
    fun `toContent falls back to the alternative text of a text block, and the text of an image block`() {
        val content = result(
            mainItems = listOf(
                item(PayPalCreditMessageItemType.TEXT, alternativeText = "Pay in 4 with "),
                item(PayPalCreditMessageItemType.IMAGE, text = "PayPal")
            )
        ).toContent()

        assertEquals("Pay in 4 with PayPal", content.message)
    }

    @Test
    fun `toContent falls back to the alternative text when the text of a text block is empty`() {
        val content = result(
            mainItems = listOf(item(PayPalCreditMessageItemType.TEXT, text = "", alternativeText = "Pay in 4"))
        ).toContent()

        assertEquals("Pay in 4", content.message)
    }

    @Test
    fun `toContent falls back to the text when the alternative text of an image block is empty`() {
        val content = result(
            mainItems = listOf(
                text("Pay in 4 with "),
                item(PayPalCreditMessageItemType.IMAGE, text = "PayPal", alternativeText = "")
            )
        ).toContent()

        assertEquals("Pay in 4 with PayPal", content.message)
    }

    @Test
    fun `toContent keeps a blank block as the space between two blocks instead of falling back`() {
        val content = result(
            mainItems = listOf(
                text("Pay in 4"),
                item(PayPalCreditMessageItemType.TEXT, text = " ", alternativeText = "ignored"),
                text("now")
            )
        ).toContent()

        assertEquals("Pay in 4 now", content.message)
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

        assertNull(content.learnMore)
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
        clickUrl: String? = null,
        isEmbeddable: Boolean? = null
    ) = PayPalCreditMessageItem(
        type = type,
        text = text,
        alternativeText = alternativeText,
        clickUrl = clickUrl,
        sourceUrl = null,
        name = null,
        isEmbeddable = isEmbeddable
    )
}
