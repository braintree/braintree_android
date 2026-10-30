package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodSummary
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** What the saved payment method component renders. */
internal sealed interface PayPalSavedPaymentMethodDisplayState {

    /** Whether the buyer can change the funding instrument. */
    val isEditable: Boolean

    /** The funding instrument is being fetched. */
    data object Loading : PayPalSavedPaymentMethodDisplayState {
        override val isEditable = false
    }

    /** A funding instrument to display. */
    data class Content(val paymentMethod: PayPalSavedPaymentMethod) : PayPalSavedPaymentMethodDisplayState {
        override val isEditable = true
    }

    /** No funding instrument, so the buyer's email is shown instead. [isEditable] comes from PayPal. */
    data class NoFundingInstrument(
        val payerEmail: String,
        override val isEditable: Boolean
    ) : PayPalSavedPaymentMethodDisplayState

    /** The fetch failed because the device is offline. Rendered the same as [Error] for now. */
    data object NoNetwork : PayPalSavedPaymentMethodDisplayState {
        override val isEditable = false
    }

    /** The fetch failed or there's nothing to show, so only the PayPal logo is shown. */
    data object Error : PayPalSavedPaymentMethodDisplayState {
        override val isEditable = false
    }

    /** Nothing is rendered, for example when the view isn't in an activity that can launch PayPal. */
    data object Hidden : PayPalSavedPaymentMethodDisplayState {
        override val isEditable = false
    }
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
    // Capped, since a cause chain can loop (A -> B -> A).
    if (generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).any { it.isConnectionFailure() }) {
        PayPalSavedPaymentMethodDisplayState.NoNetwork
    } else {
        PayPalSavedPaymentMethodDisplayState.Error
    }

// An SSLException on its own (e.g. a pinning failure) isn't offline, so it isn't matched here. One caused by a
// socket error, a connection dropped during the TLS handshake, still maps to NoNetwork through its cause.
private fun Throwable.isConnectionFailure(): Boolean =
    this is UnknownHostException || this is SocketException || this is SocketTimeoutException

private const val MAX_CAUSE_DEPTH = 10
