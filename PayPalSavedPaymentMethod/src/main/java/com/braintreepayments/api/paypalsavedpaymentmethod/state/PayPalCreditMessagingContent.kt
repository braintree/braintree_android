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
    /** The "Learn more" link. Text and URL always come together, as on iOS. */
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

// Like iOS: image blocks read their alternative text, other blocks their text, each falling back to the other.
// Unlike iOS, an empty string counts as missing too. A blank string doesn't: it can be the space between two blocks.
private fun PayPalCreditMessageItem.displayText(): String {
    val isImage = type == PayPalCreditMessageItemType.IMAGE
    val preferred = if (isImage) alternativeText else text
    val fallback = if (isImage) text else alternativeText
    return preferred?.takeIf { it.isNotEmpty() } ?: fallback.orEmpty()
}

// Like iOS: the first action item of any type with text and a URL. Android only accepts https URLs.
private fun PayPalCreditMessageItem.toLearnMore(): PayPalCreditMessagingContent.LearnMore? {
    val linkText = text?.takeIf { it.isNotBlank() } ?: alternativeText?.takeIf { it.isNotBlank() } ?: return null
    val url = clickUrl.asHttpsUrlOrNull() ?: return null
    return PayPalCreditMessagingContent.LearnMore(linkText, url, isEmbeddable == true)
}
