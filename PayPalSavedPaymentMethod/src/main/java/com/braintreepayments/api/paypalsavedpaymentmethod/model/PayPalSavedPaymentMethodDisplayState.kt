package com.braintreepayments.api.paypalsavedpaymentmethod.model

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** What the saved payment method component renders. */
internal sealed interface PayPalSavedPaymentMethodDisplayState {

    /** The funding instrument is being fetched. */
    data object Loading : PayPalSavedPaymentMethodDisplayState

    /** A funding instrument to display. */
    data class Content(val paymentMethod: PayPalSavedPaymentMethod) : PayPalSavedPaymentMethodDisplayState

    /** No funding instrument, so the buyer's email is shown instead. */
    data class NoFundingInstrument(
        val payerEmail: String,
        val isEditable: Boolean
    ) : PayPalSavedPaymentMethodDisplayState

    /** The fetch failed because the device is offline. Rendered the same as [Error] for now. */
    data object NoNetwork : PayPalSavedPaymentMethodDisplayState

    /** The fetch failed or there's nothing to show, so only the PayPal logo is shown. */
    data object Error : PayPalSavedPaymentMethodDisplayState

    /** Nothing is rendered, for example when the view isn't in an activity that can launch PayPal. */
    data object Hidden : PayPalSavedPaymentMethodDisplayState
}

/** Maps a fetched summary to the first funding instrument, otherwise the buyer's email, otherwise an error. */
internal fun PayPalSavedPaymentMethodSummary.toDisplayState(): PayPalSavedPaymentMethodDisplayState {
    val paymentMethod = paymentMethods.firstOrNull()
    return when {
        paymentMethod != null -> PayPalSavedPaymentMethodDisplayState.Content(paymentMethod)
        payer != null -> PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(payer.email, payer.isEditable)
        else -> PayPalSavedPaymentMethodDisplayState.Error
    }
}

/** Maps a failed fetch to no network for a connection failure, otherwise an error. Callers re-throw cancellation. */
internal fun Throwable.toDisplayState(): PayPalSavedPaymentMethodDisplayState =
    if (generateSequence(this) { it.cause }.any { it.isConnectionFailure() }) {
        PayPalSavedPaymentMethodDisplayState.NoNetwork
    } else {
        PayPalSavedPaymentMethodDisplayState.Error
    }

// SSLException is left out: it also covers pinning failures, which aren't an offline case.
private fun Throwable.isConnectionFailure(): Boolean =
    this is UnknownHostException || this is SocketException || this is SocketTimeoutException
