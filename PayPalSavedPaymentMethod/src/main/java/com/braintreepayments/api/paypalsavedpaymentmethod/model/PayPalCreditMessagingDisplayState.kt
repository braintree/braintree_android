package com.braintreepayments.api.paypalsavedpaymentmethod.model

/** What the Pay Later row renders. Fetched separately from the funding instrument. */
internal sealed interface PayPalCreditMessagingDisplayState {

    /** The message is being fetched. */
    data object Loading : PayPalCreditMessagingDisplayState

    /** A message to display. */
    data class Content(val content: PayPalCreditMessagingContent) : PayPalCreditMessagingDisplayState

    /** No row is shown. */
    data object Hidden : PayPalCreditMessagingDisplayState
}

/** Maps a fetched Pay Later message to what the row renders. A missing or blank message hides the row. */
internal fun PayPalCreditMessagingResult?.toDisplayState(): PayPalCreditMessagingDisplayState {
    val content = this?.toContent() ?: return PayPalCreditMessagingDisplayState.Hidden
    return if (content.message.isBlank()) {
        PayPalCreditMessagingDisplayState.Hidden
    } else {
        PayPalCreditMessagingDisplayState.Content(content)
    }
}
