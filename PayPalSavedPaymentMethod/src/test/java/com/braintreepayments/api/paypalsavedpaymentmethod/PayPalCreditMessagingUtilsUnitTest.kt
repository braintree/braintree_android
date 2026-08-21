package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalCreditMessagingUtilsUnitTest {

    @Test
    fun message_withTextAndImageMainItems_substitutesAlternativeTextForImages() {
        val mainItems = listOf(
            ContentItem(type = "TEXT", text = "Or pay in 4 interest-free payments with ", alternativeText = ""),
            ContentItem(type = "IMAGE", text = "", alternativeText = "PayPal"),
            ContentItem(type = "TEXT", text = ".", alternativeText = "")
        )

        val result = PayPalCreditMessagingUtils.message(mainItems, disclaimerItems = emptyList())

        assertEquals("Or pay in 4 interest-free payments with PayPal.", result)
    }

    @Test
    fun message_withDisclaimerItems_appendsAfterMainItemsWithSpace() {
        val mainItems = listOf(ContentItem(type = "TEXT", text = "As low as \$10/mo", alternativeText = ""))
        val disclaimerItems = listOf(
            ContentItem(type = "TEXT", text = "Available to US residents only.", alternativeText = "")
        )

        val result = PayPalCreditMessagingUtils.message(mainItems, disclaimerItems)

        assertEquals("As low as \$10/mo Available to US residents only.", result)
    }

    @Test
    fun message_withoutDisclaimerItems_returnsMainItemsOnly() {
        val mainItems = listOf(ContentItem(type = "TEXT", text = "As low as \$10/mo", alternativeText = ""))

        val result = PayPalCreditMessagingUtils.message(mainItems, disclaimerItems = emptyList())

        assertEquals("As low as \$10/mo", result)
    }

    @Test
    fun message_withoutMainOrDisclaimerItems_returnsEmptyMessage() {
        val result = PayPalCreditMessagingUtils.message(mainItems = emptyList(), disclaimerItems = emptyList())

        assertEquals("", result)
    }

    @Test
    fun learnMoreText_withLinkActionItem_extractsText() {
        val actionItems = listOf(ActionItem(type = "LINK", text = "Learn more", url = "https://paypal.com/learn"))

        assertEquals("Learn more", PayPalCreditMessagingUtils.learnMoreText(actionItems))
    }

    @Test
    fun learnMoreUrl_withLinkActionItem_extractsUrl() {
        val actionItems = listOf(ActionItem(type = "LINK", text = "Learn more", url = "https://paypal.com/learn"))

        assertEquals("https://paypal.com/learn", PayPalCreditMessagingUtils.learnMoreUrl(actionItems))
    }

    @Test
    fun learnMoreText_withMultipleActionItems_picksFirstLinkType() {
        val actionItems = listOf(
            ActionItem(type = "OTHER", text = "Ignore me", url = "https://paypal.com/ignore"),
            ActionItem(type = "LINK", text = "Learn more", url = "https://paypal.com/learn")
        )

        assertEquals("Learn more", PayPalCreditMessagingUtils.learnMoreText(actionItems))
        assertEquals("https://paypal.com/learn", PayPalCreditMessagingUtils.learnMoreUrl(actionItems))
    }

    @Test
    fun learnMoreText_withoutActionItems_returnsEmpty() {
        assertEquals("", PayPalCreditMessagingUtils.learnMoreText(emptyList()))
    }

    @Test
    fun learnMoreUrl_withoutActionItems_returnsEmpty() {
        assertEquals("", PayPalCreditMessagingUtils.learnMoreUrl(emptyList()))
    }
}
