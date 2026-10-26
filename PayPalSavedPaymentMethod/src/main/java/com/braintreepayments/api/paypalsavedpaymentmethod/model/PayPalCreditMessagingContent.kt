package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull

/** The Pay Later message ready to render. [message] is empty when PayPal returns no main text. */
internal data class PayPalCreditMessagingContent(
    val message: String,
    val learnMoreText: String?,
    val learnMoreUrl: String?,
    val messageId: String? = null,
    val messageType: String? = null,
    val impressionUrl: String? = null
)

/** Maps a Pay Later message to [PayPalCreditMessagingContent], keeping only https links. */
internal fun PayPalCreditMessagingResult.toContent(): PayPalCreditMessagingContent {
    val main = mainItems.displayText()
    val disclaimer = disclaimerItems.displayText()
    val link = actionItems.firstOrNull {
        it.type == PayPalCreditMessageItemType.LINK && !it.linkText().isNullOrBlank() &&
            it.clickUrl.asHttpsUrlOrNull() != null
    }
    val message = if (main.isBlank()) {
        ""
    } else {
        listOf(main, disclaimer).filter { it.isNotBlank() }.joinToString(" ") { it.trim() }
    }
    return PayPalCreditMessagingContent(
        message = message,
        learnMoreText = link?.linkText(),
        learnMoreUrl = link?.clickUrl,
        messageId = messageId,
        messageType = messageType,
        impressionUrl = impressionUrl.asHttpsUrlOrNull()
    )
}

private fun List<PayPalCreditMessageItem>.displayText(): String =
    joinToString("") { item ->
        if (item.type == PayPalCreditMessageItemType.IMAGE) item.alternativeText.orEmpty() else item.text.orEmpty()
    }

private fun PayPalCreditMessageItem.linkText(): String? = text?.takeIf { it.isNotBlank() } ?: alternativeText
