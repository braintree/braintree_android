package com.braintreepayments.api.paypalsavedpaymentmethod.model

/**
 * The Pay Later message ready to render. Shared by the XML and Compose views.
 *
 * @property message The text to display: the main blocks followed by the disclaimer, if any. Image blocks such as the
 * PayPal logo contribute their alternative text, since the component already shows the logo.
 * @property learnMoreText The "Learn more" link text, or `null` when PayPal returns no link.
 * @property learnMoreUrl The https URL the "Learn more" link opens, or `null` when PayPal returns no link.
 */
internal data class PayPalCreditMessagingContent(
    val message: String,
    val learnMoreText: String?,
    val learnMoreUrl: String?
)

/**
 * Maps a Pay Later message to [PayPalCreditMessagingContent]. The link is the first [PayPalCreditMessageItemType.LINK]
 * action with an https URL and text, or alternative text when it has no text.
 */
internal fun PayPalCreditMessagingResult.toContent(): PayPalCreditMessagingContent {
    val main = mainItems.displayText()
    val disclaimer = disclaimerItems.displayText()
    val link = actionItems.firstOrNull {
        it.type == PayPalCreditMessageItemType.LINK && !it.linkText().isNullOrBlank() && it.clickUrl != null
    }
    return PayPalCreditMessagingContent(
        // Main blocks carry their own spacing; the disclaimer is a separate sentence.
        message = if (disclaimer.isBlank()) main else "$main $disclaimer",
        learnMoreText = link?.linkText(),
        learnMoreUrl = link?.clickUrl
    )
}

private fun List<PayPalCreditMessageItem>.displayText(): String =
    joinToString("") { item ->
        if (item.type == PayPalCreditMessageItemType.IMAGE) item.alternativeText.orEmpty() else item.text.orEmpty()
    }

private fun PayPalCreditMessageItem.linkText(): String? = text ?: alternativeText
