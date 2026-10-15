package com.braintreepayments.api.paypalsavedpaymentmethod.model

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * What the saved payment method component renders. Shared by the XML and Compose views.
 */
internal sealed interface PayPalSavedPaymentMethodDisplayState {

    /** The funding instrument is being fetched. */
    data object Loading : PayPalSavedPaymentMethodDisplayState

    /** A funding instrument to display, with its icon, label or last digits, and the edit control. */
    data class Content(val paymentMethod: PayPalSavedPaymentMethod) : PayPalSavedPaymentMethodDisplayState

    /**
     * PayPal returned no funding instrument but did return the buyer's email, which is displayed instead.
     * [isEditable] is whether the buyer can change the funding instrument.
     */
    data class NoFundingInstrument(
        val payerEmail: String,
        val isEditable: Boolean
    ) : PayPalSavedPaymentMethodDisplayState

    /** The funding instrument could not be fetched because the device is offline. The PayPal logo stays visible. */
    data object NoNetwork : PayPalSavedPaymentMethodDisplayState

    /** The funding instrument could not be fetched or there is nothing to display. The PayPal logo stays visible. */
    data object Error : PayPalSavedPaymentMethodDisplayState

    /**
     * Nothing is rendered, not even the PayPal logo. Used after an edit when the funding instrument the buyer picked
     * can't be shown, since the one shown before the edit is no longer the one that will be charged.
     */
    data object Hidden : PayPalSavedPaymentMethodDisplayState
}

/**
 * Maps a fetched summary to what the component renders: the first funding instrument, otherwise the buyer's email,
 * otherwise [PayPalSavedPaymentMethodDisplayState.Error].
 */
internal fun PayPalSavedPaymentMethodSummary.toDisplayState(): PayPalSavedPaymentMethodDisplayState {
    val paymentMethod = paymentMethods.firstOrNull()
    val payer = payer
    return when {
        paymentMethod != null -> PayPalSavedPaymentMethodDisplayState.Content(paymentMethod)
        payer != null -> PayPalSavedPaymentMethodDisplayState.NoFundingInstrument(payer.email, payer.isEditable)
        else -> PayPalSavedPaymentMethodDisplayState.Error
    }
}

/**
 * Maps a failed fetch to what the component renders. Only connection failures count as
 * [PayPalSavedPaymentMethodDisplayState.NoNetwork]; server and parsing errors, including a `BraintreeException`, which
 * is an `IOException`, map to [PayPalSavedPaymentMethodDisplayState.Error].
 */
internal fun Throwable.toDisplayState(): PayPalSavedPaymentMethodDisplayState =
    if (generateSequence(this) { it.cause }.any { it.isConnectionFailure() }) {
        PayPalSavedPaymentMethodDisplayState.NoNetwork
    } else {
        PayPalSavedPaymentMethodDisplayState.Error
    }

private fun Throwable.isConnectionFailure(): Boolean =
    this is UnknownHostException || this is ConnectException || this is SocketTimeoutException
