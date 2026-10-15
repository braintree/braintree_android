package com.braintreepayments.api.paypalsavedpaymentmethod.model

/**
 * What the Pay Later row renders. Separate from [PayPalSavedPaymentMethodDisplayState] because the message is fetched
 * on its own and never blocks the funding instrument row. Shared by the XML and Compose views.
 */
internal sealed interface PayPalCreditMessagingDisplayState {

    /** The message is being fetched. */
    data object Loading : PayPalCreditMessagingDisplayState

    /** A message to display. */
    data class Content(val content: PayPalCreditMessagingContent) : PayPalCreditMessagingDisplayState

    /**
     * No row: PayPal returned no message, the fetch failed, the merchant turned the message off, or the funding
     * instrument could not be shown.
     */
    data object Hidden : PayPalCreditMessagingDisplayState
}

/**
 * Maps a fetched Pay Later message to what the row renders. A `null` result or a blank message hides the row.
 */
internal fun PayPalCreditMessagingResult?.toDisplayState(): PayPalCreditMessagingDisplayState {
    val content = this?.toContent() ?: return PayPalCreditMessagingDisplayState.Hidden
    return if (content.message.isBlank()) {
        PayPalCreditMessagingDisplayState.Hidden
    } else {
        PayPalCreditMessagingDisplayState.Content(content)
    }
}
