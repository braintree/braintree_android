package com.braintreepayments.api.paypalsavedpaymentmethod.model

import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull

/**
 * The Pay Later message ready to render. Shared by the XML and Compose views.
 *
 * @property message The text to display: the main blocks followed by the disclaimer, if any. Image blocks such as the
 * PayPal logo contribute their alternative text, since the component already shows the logo. Empty when PayPal returns
 * no main text, since the disclaimer alone has nothing to qualify.
 * @property learnMoreText The "Learn more" link text, or `null` when PayPal returns no link.
 * @property learnMoreUrl The https URL the "Learn more" link opens, or `null` when PayPal returns no link.
 * @property messageId The identifier of the message PayPal selected, for analytics.
 * @property messageType The template the message was built from, for example `"PLST_SQ"`, for analytics.
 * @property impressionUrl The https beacon to fire once the message is on screen, or `null` when PayPal returns none.
 */
internal data class PayPalCreditMessagingContent(
    val message: String,
    val learnMoreText: String?,
    val learnMoreUrl: String?,
    val messageId: String? = null,
    val messageType: String? = null,
    val impressionUrl: String? = null
)

/**
 * Maps a Pay Later message to [PayPalCreditMessagingContent]. The link is the first [PayPalCreditMessageItemType.LINK]
 * action with an https URL and text, or alternative text when it has no text.
 *
 * Parsing already drops non-https URLs, but [learnMoreUrl][PayPalCreditMessagingContent.learnMoreUrl] and
 * [impressionUrl][PayPalCreditMessagingContent.impressionUrl] are validated again here with [asHttpsUrlOrNull], so
 * the URLs the views open or fire are https however the result was built.
 */
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
        // The disclaimer is a separate sentence; parts are trimmed so a blank one adds no stray space.
        listOf(main, disclaimer).filter { it.isNotBlank() }.joinToString(" ") { it.trim() }
    }
    return PayPalCreditMessagingContent(
        message = message,
        learnMoreText = link?.linkText(),
        learnMoreUrl = link?.clickUrl.asHttpsUrlOrNull(),
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
