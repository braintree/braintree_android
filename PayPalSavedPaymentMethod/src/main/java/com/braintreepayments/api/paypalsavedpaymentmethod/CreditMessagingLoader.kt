package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toDisplayState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Fetches the Pay Later message and puts it in [state]. Only the latest [fetch] counts, so an older amount's message
 * can't land last. The message stays hidden after an edit, since the offer no longer applies.
 */
@OptIn(ExperimentalBetaApi::class)
// The client reports failures by throwing, so its calls catch Exception and rethrow cancellation, as CardClient does.
@Suppress("TooGenericExceptionCaught")
internal class CreditMessagingLoader(
    private val client: PayPalSavedPaymentMethodClient,
    private val state: MutableStateFlow<PayPalSavedPaymentMethodUiState>,
    scope: CoroutineScope
) {

    private data class Requested(val request: PayPalSavedPaymentMethodRequest, val showCreditMessaging: Boolean)

    // The latest is replayed, so one asked for before the collector starts isn't lost.
    private val requests = MutableSharedFlow<Requested>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // The request whose message was loaded and is shown; null when none is.
    private var loadedRequest: PayPalSavedPaymentMethodRequest? = null

    init {
        // collectLatest cancels the fetch for older requests.
        scope.launch { requests.collectLatest { show(it) } }
    }

    /** Fetches the message for [request], or hides it when [showCreditMessaging] is off. */
    fun fetch(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        requests.tryEmit(Requested(request, showCreditMessaging))
    }

    /** Like [fetch], but keeps a message already loaded for [request] instead of fetching it again. */
    fun fetchIfNotShown(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        if (!showCreditMessaging || request != loadedRequest) fetch(request, showCreditMessaging)
    }

    private suspend fun show(requested: Requested) {
        val hidden = PayPalCreditMessagingDisplayState.Hidden
        // Hidden when turned off, and after an edit, since the offer no longer applies.
        if (!requested.showCreditMessaging || state.value.isEditCompleted) {
            // Nothing is shown any more, so turning the message back on fetches it.
            loadedRequest = null
            state.update { it.copy(creditMessaging = hidden) }
            return
        }
        val request = requested.request
        val creditMessaging = try {
            client.fetchCreditPresentmentMessages(request.amount, request.currencyCode).toDisplayState()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // Hidden, and not recorded as loaded, so the next view that binds retries it.
            loadedRequest = null
            state.update { it.copy(creditMessaging = hidden) }
            return
        }
        // An edit that completed during the fetch has already hidden the offer.
        if (state.value.isEditCompleted) return
        loadedRequest = request
        state.update { it.copy(creditMessaging = creditMessaging) }
    }
}
