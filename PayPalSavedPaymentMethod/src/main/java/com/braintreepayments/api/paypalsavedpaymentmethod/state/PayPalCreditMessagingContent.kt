package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessageItem
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessageItemType
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingResult

/** The Pay Later message ready to render. [message] is empty when PayPal returns no main text. */
internal data class PayPalCreditMessagingContent(
    val message: String,
    val learnMore: LearnMore?,
    val messageId: String? = null,
    val messageType: String? = null,
    val impressionUrl: String? = null
) {
    /** The "Learn more" link; text and URL always come together. */
    internal data class LearnMore(val text: String, val url: String, val isEmbeddable: Boolean)
}

/**
 * Maps a Pay Later message to [PayPalCreditMessagingContent], keeping only https links. Parsing already drops other
 * URLs; they're checked again here so the content doesn't depend on how the result was built.
 */
internal fun PayPalCreditMessagingResult.toContent(): PayPalCreditMessagingContent {
    val main = mainItems.displayText()
    val disclaimer = disclaimerItems.displayText()
    val message = if (main.isBlank()) {
        ""
    } else {
        listOf(main, disclaimer).filter { it.isNotBlank() }.joinToString(" ") { it.trim() }
    }
    return PayPalCreditMessagingContent(
        message = message,
        learnMore = actionItems.firstNotNullOfOrNull { it.toLearnMore() },
        messageId = messageId,
        messageType = messageType,
        impressionUrl = impressionUrl.asHttpsUrlOrNull()
    )
}

private fun List<PayPalCreditMessageItem>.displayText(): String = joinToString("") { it.displayText() }

// Image blocks read their alternative text, other blocks their text; each falls back to the other. An empty string
// counts as missing, a blank one doesn't: it can be the space between two blocks.
private fun PayPalCreditMessageItem.displayText(): String {
    val isImage = type == PayPalCreditMessageItemType.IMAGE
    val preferred = if (isImage) alternativeText else text
    val fallback = if (isImage) text else alternativeText
    return preferred?.takeIf { it.isNotEmpty() } ?: fallback.orEmpty()
}

// The first action item of any type that has text and an https URL.
private fun PayPalCreditMessageItem.toLearnMore(): PayPalCreditMessagingContent.LearnMore? {
    val linkText = text?.takeIf { it.isNotBlank() } ?: alternativeText?.takeIf { it.isNotBlank() } ?: return null
    val url = clickUrl.asHttpsUrlOrNull() ?: return null
    return PayPalCreditMessagingContent.LearnMore(linkText, url, isEmbeddable == true)
}
