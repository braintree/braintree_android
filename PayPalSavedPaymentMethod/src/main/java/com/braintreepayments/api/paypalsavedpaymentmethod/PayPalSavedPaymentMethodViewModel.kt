package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toDisplayState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.MessageDigest

/** What [PayPalSavedPaymentMethodViewModel.handleReturnToApp] did with a PayPal return. */
internal enum class PayPalReturnHandling {
    /** Not taken, because an edit step is running. Keep the pending request and pass it again. */
    NOT_TAKEN,

    /** Taken. Forget the pending request; the intent holds no used-up PayPal return URL. */
    TAKEN,

    /** Taken, and the return is completed. Forget the pending request and clear the return URL from the intent. */
    COMPLETED
}

/**
 * State and edit flow shared by the XML and Compose views, for one authorization. The state
 * survives configuration changes and the completed edit survives process death. A PayPal return is completed at most
 * once, so a result lost with the process isn't delivered again. Main thread only.
 */
@OptIn(ExperimentalBetaApi::class)
@Suppress("TooManyFunctions")
internal class PayPalSavedPaymentMethodViewModel(
    private val savedState: SavedStateHandle,
    private val client: PayPalSavedPaymentMethodClient
) : ViewModel() {

    private val _state = MutableStateFlow(
        PayPalSavedPaymentMethodUiState(isEditCompleted = savedState[IS_EDIT_COMPLETED_KEY] ?: false)
    )
    val state: StateFlow<PayPalSavedPaymentMethodUiState> = _state.asStateFlow()

    // Results no view has handled yet, oldest first.
    private val pendingResults = MutableStateFlow<List<PayPalResult>>(emptyList())

    /** Delivers each result at least once: a result whose collector is cancelled goes to the next collector. */
    val results: Flow<PayPalResult> = flow {
        pendingResults.collect { pending ->
            val result = pending.firstOrNull() ?: return@collect
            try {
                emit(result)
            } finally {
                // Still active means the collector handled it.
                if (currentCoroutineContext().isActive) {
                    pendingResults.update { if (it.firstOrNull() === result) it.drop(1) else it }
                }
            }
        }
    }

    // What the views asked for; null until the first load.
    private val inputs = MutableStateFlow<Inputs?>(null)
    private var defaultFundingInstrumentJob: Job? = null
    private var updatedFundingInstrumentJob: Job? = null

    // Only in saved state, since the views don't show it.
    private var editedOrderId: String?
        get() = savedState[EDITED_ORDER_ID_KEY]
        set(value) {
            savedState[EDITED_ORDER_ID_KEY] = value
        }

    // Allows one edit step at a time. The views show it as isEditing.
    private var isEditStepRunning = false
        set(value) {
            field = value
            _state.update { it.copy(isEditing = value) }
        }

    init {
        viewModelScope.launch { showCreditMessagingForInputs() }
    }

    /**
     * Loads the default funding instrument and, if enabled, the Pay Later message. Called each time a view binds, so a
     * failed load is retried and a shown one refreshed. After an edit the funding instrument isn't refetched, since the
     * API still returns the one from before the edit; only an edit restored from [savedState] loads it once.
     */
    fun load(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        val previous = inputs.value
        // A new load count refreshes the Pay Later message even for the same request.
        inputs.value = Inputs(request, showCreditMessaging, loadCount = (previous?.loadCount ?: 0) + 1)
        if (!state.value.isEditCompleted) {
            loadBuyerDefaultBillingAgreement()
        } else if (previous == null) {
            loadBuyerUpdatedBillingAgreement()
        }
    }

    /** Reloads or hides the Pay Later message when the request or its setting changes. Does nothing before [load]. */
    fun onRequestChanged(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        // Unchanged values don't emit, so they reload nothing.
        inputs.update { it?.copy(request = request, showCreditMessaging = showCreditMessaging) }
    }

    /**
     * Opens the PayPal paysheet from [context]'s activity. Returns the pending request to keep, a failure to report,
     * or `null` if an edit step is already running or the activity closed meanwhile.
     */
    suspend fun startEdit(
        context: Context,
        payPalLauncher: PayPalLauncher,
        payPalCheckoutRequest: PayPalCheckoutRequest
    ): PayPalPendingRequest? {
        // TODO: send the edit flow analytics events, here and in handleReturnToApp, once the catalog is approved.
        if (isEditStepRunning) return null
        isEditStepRunning = true
        return try {
            tryOrElse({
                val activity = context.findActivity() as? ComponentActivity
                    ?: return PayPalPendingRequest.Failure(BraintreeException(NO_COMPONENT_ACTIVITY_MESSAGE))
                when (val authRequest = client.createEditFundingInstrumentAuthRequest(context, payPalCheckoutRequest)) {
                    is PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch ->
                        // There's no view left to return to.
                        if (activity.isFinishing || activity.isDestroyed) {
                            null
                        } else {
                            payPalLauncher.launch(activity, authRequest.paymentAuthRequest)
                        }
                    is PayPalSavedPaymentMethodAuthRequest.Failure -> PayPalPendingRequest.Failure(authRequest.error)
                }
            }) { error -> PayPalPendingRequest.Failure(error) }
        } finally {
            isEditStepRunning = false
        }
    }

    /**
     * Reads the PayPal return in [intent] and delivers the result through [results]. Tokenizing runs in the view
     * model's scope, so recreating the view doesn't cancel or repeat it. A return with no result is a
     * [PayPalResult.Cancel] and can be completed later. After a success the funding instrument the buyer picked is
     * loaded. A return already completed, including before process death, isn't completed again.
     */
    fun handleReturnToApp(
        pendingRequest: String,
        payPalLauncher: PayPalLauncher,
        intent: Intent?
    ): PayPalReturnHandling {
        if (pendingRequest.digest() == savedState[COMPLETED_PENDING_REQUEST_KEY]) return PayPalReturnHandling.COMPLETED
        if (isEditStepRunning) return PayPalReturnHandling.NOT_TAKEN
        if (intent == null) {
            deliver(PayPalResult.Failure(BraintreeException(NO_INTENT_MESSAGE)))
            return PayPalReturnHandling.TAKEN
        }
        return tryOrElse({
            payPalLauncher.restorePendingRequest(pendingRequest)
            val started = PayPalPendingRequest.Started(pendingRequest)
            when (val authResult = payPalLauncher.handleReturnToApp(started, intent)) {
                is PayPalPaymentAuthResult.Success -> {
                    // Recorded before tokenizing, which uses up the PayPal approval, so it's never tokenized twice.
                    markCompleted(pendingRequest)
                    tokenize(authResult)
                    PayPalReturnHandling.COMPLETED
                }
                is PayPalPaymentAuthResult.Failure -> failReturn(pendingRequest, authResult.error)
                PayPalPaymentAuthResult.NoResult -> {
                    deliver(PayPalResult.Cancel)
                    PayPalReturnHandling.TAKEN
                }
            }
        }) { error -> failReturn(pendingRequest, error) }
    }

    private fun tokenize(authResult: PayPalPaymentAuthResult.Success) {
        isEditStepRunning = true
        viewModelScope.launch {
            val result = try {
                tryOrElse({ client.tokenize(authResult) }) { error -> PayPalResult.Failure(error) }
            } finally {
                isEditStepRunning = false
            }
            when (result) {
                is PayPalResult.Success -> {
                    saveEdit(editedOrderId = result.nonce.paymentId)
                    loadBuyerUpdatedBillingAgreement()
                }
                // A cancel on the PayPal page doesn't use up the return, so the buyer can complete it later.
                PayPalResult.Cancel -> savedState.remove<String>(COMPLETED_PENDING_REQUEST_KEY)
                is PayPalResult.Failure -> Unit
            }
            deliver(result)
        }
    }

    private fun failReturn(pendingRequest: String, error: Exception): PayPalReturnHandling {
        markCompleted(pendingRequest)
        deliver(PayPalResult.Failure(error))
        return PayPalReturnHandling.COMPLETED
    }

    private fun deliver(result: PayPalResult) {
        pendingResults.update { it + result }
    }

    // A digest, since it's only compared for equality.
    private fun markCompleted(pendingRequest: String) {
        savedState[COMPLETED_PENDING_REQUEST_KEY] = pendingRequest.digest()
    }

    private fun String.digest(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun saveEdit(editedOrderId: String?) {
        savedState[IS_EDIT_COMPLETED_KEY] = true
        this.editedOrderId = editedOrderId
        // The Pay Later offer was quoted for the funding instrument from before the edit.
        _state.update {
            it.copy(isEditCompleted = true, creditMessaging = PayPalCreditMessagingDisplayState.Hidden)
        }
    }

    private fun loadBuyerDefaultBillingAgreement() {
        val merchantAccountId = inputs.value?.request?.merchantAccountId
        defaultFundingInstrumentJob?.cancel()
        defaultFundingInstrumentJob = viewModelScope.launch {
            val fundingInstrument = tryOrElse({
                client.fetchPaymentMethod(
                    PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
                    null,
                    merchantAccountId
                ).toDisplayState()
            }) { error ->
                // A failed refresh keeps what's shown; only a failed first load shows the error.
                if (state.value.fundingInstrument != PayPalSavedPaymentMethodDisplayState.Loading) return@launch
                error.toDisplayState()
            }
            // An edit completed meanwhile, and its funding instrument wins.
            if (state.value.isEditCompleted) return@launch
            _state.update { it.copy(fundingInstrument = fundingInstrument) }
        }
    }

    // If the picked funding instrument can't be loaded only the brand shows, since the old one is no longer charged.
    private fun loadBuyerUpdatedBillingAgreement() {
        val merchantAccountId = inputs.value?.request?.merchantAccountId
        updatedFundingInstrumentJob?.cancel()
        val orderId = editedOrderId
        if (orderId == null) {
            _state.update { it.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Error) }
            return
        }
        _state.update { it.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Loading) }
        updatedFundingInstrumentJob = viewModelScope.launch {
            val fundingInstrument = tryOrElse({
                client.fetchPaymentMethod(
                    PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT,
                    orderId,
                    merchantAccountId
                ).toDisplayState()
            }) { error -> error.toDisplayState() }
            _state.update { it.copy(fundingInstrument = fundingInstrument) }
        }
    }

    // Shows the Pay Later message for the latest inputs. collectLatest cancels the fetch for older inputs, so an older
    // amount's message can't land last.
    private suspend fun showCreditMessagingForInputs() {
        // The request the shown message was quoted for.
        var shownFor: PayPalSavedPaymentMethodRequest? = null
        inputs.filterNotNull().collectLatest { input ->
            if (!input.showCreditMessaging || state.value.isEditCompleted) {
                _state.update { it.copy(creditMessaging = PayPalCreditMessagingDisplayState.Hidden) }
                return@collectLatest
            }
            val request = input.request
            val creditMessaging = tryOrElse({
                client.fetchCreditPresentmentMessages(request.amount, request.currencyCode).toDisplayState()
            }) {
                // A failed refresh for the same request keeps the message; any other failure hides it.
                if (request == shownFor) return@collectLatest
                PayPalCreditMessagingDisplayState.Hidden
            }
            // An edit completed meanwhile, and the offer no longer applies.
            if (state.value.isEditCompleted) return@collectLatest
            shownFor = request
            _state.update { it.copy(creditMessaging = creditMessaging) }
        }
    }

    private data class Inputs(
        val request: PayPalSavedPaymentMethodRequest,
        val showCreditMessaging: Boolean,
        val loadCount: Int
    )

    /** Creates the view model with the client from [createClient], restoring the completed edit from saved state. */
    class Factory(
        private val createClient: () -> PayPalSavedPaymentMethodClient
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedState = try {
                extras.createSavedStateHandle()
            } catch (ignored: IllegalArgumentException) {
                // No saved state owner, so nothing survives process death.
                SavedStateHandle()
            }
            return PayPalSavedPaymentMethodViewModel(savedState, createClient()) as T
        }
    }

    companion object {
        const val NO_COMPONENT_ACTIVITY_MESSAGE =
            "PayPalSavedPaymentMethodView must be hosted in a ComponentActivity."
        const val NO_INTENT_MESSAGE = "Unable to read the return from PayPal: no intent was given."
        private const val IS_EDIT_COMPLETED_KEY = "is_edit_completed"
        private const val EDITED_ORDER_ID_KEY = "edited_order_id"
        private const val COMPLETED_PENDING_REQUEST_KEY = "completed_pending_request"
    }
}

/**
 * Runs [block], passing any failure other than cancellation to [onError]. The client reports failures by throwing, and
 * cancellation must still reach the coroutine.
 */
@Suppress("TooGenericExceptionCaught")
private inline fun <T> tryOrElse(block: () -> T, onError: (Exception) -> T): T =
    try {
        block()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        onError(e)
    }
