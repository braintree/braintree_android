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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What [PayPalSavedPaymentMethodViewModel.handleReturnToApp] did with a PayPal return. */
internal enum class PayPalSavedPaymentMethodReturnResult {
    /** Not taken, because an edit step is running. Keep the pending request and pass it again. */
    NOT_TAKEN,

    /** Taken without completing the return. Forget the pending request and leave the intent as it is. */
    TAKEN,

    /** Taken and completed. Forget the pending request and clear the PayPal return URL from the intent. */
    COMPLETED
}

/**
 * State and edit flow shared by the XML and Compose views, for one authorization. The state survives configuration
 * changes and the completed edit survives process death. A PayPal return is completed at most once, so a result lost
 * with the process isn't delivered again. Main thread only.
 */
@OptIn(ExperimentalBetaApi::class)
// The client reports failures by throwing, so its calls catch Exception and rethrow cancellation, as CardClient does.
@Suppress("TooGenericExceptionCaught")
internal class PayPalSavedPaymentMethodViewModel(
    private val savedState: SavedStateHandle,
    private val client: PayPalSavedPaymentMethodClient
) : ViewModel() {

    // A completed edit is also kept in saved state, written together with the state in saveEdit.
    private val _state = MutableStateFlow(
        PayPalSavedPaymentMethodUiState(isEditCompleted = savedState[IS_EDIT_COMPLETED_KEY] ?: false)
    )
    val state: StateFlow<PayPalSavedPaymentMethodUiState> = _state.asStateFlow()

    private val pendingResults = PendingResults()
    private val completedReturns = CompletedReturns(savedState)
    private val creditMessagingLoader = CreditMessagingLoader(client, _state, viewModelScope)

    /** Delivers each result at least once: a result whose collector is cancelled goes to the next collector. */
    val results: Flow<PayPalResult> = pendingResults.results

    // What the views asked for; null until the first load.
    private var inputs: Inputs? = null
    private var defaultFundingInstrumentJob: Job? = null
    private var updatedFundingInstrumentJob: Job? = null

    /**
     * Loads the default funding instrument and, if enabled, the Pay Later message. Called each time a view binds, so a
     * failed load is retried and a shown one refreshed. After an edit the funding instrument isn't refetched, since the
     * API still returns the one from before the edit; only an edit restored from [savedState] loads it once.
     */
    fun load(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        val isFirstLoad = inputs == null
        inputs = Inputs(request, showCreditMessaging)
        creditMessagingLoader.fetch(request, showCreditMessaging)
        if (!state.value.isEditCompleted) {
            loadBuyerDefaultBillingAgreement()
        } else if (isFirstLoad) {
            loadBuyerUpdatedBillingAgreement()
        }
    }

    /** Reloads or hides the Pay Later message when the request or its setting changes. Does nothing before [load]. */
    fun onRequestChanged(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        val changed = Inputs(request, showCreditMessaging)
        if (inputs != null && inputs != changed) {
            inputs = changed
            creditMessagingLoader.fetch(request, showCreditMessaging)
        }
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
        if (state.value.isEditing) return null
        val activity = context.findActivity() as? ComponentActivity
            ?: return PayPalPendingRequest.Failure(BraintreeException(NO_COMPONENT_ACTIVITY_MESSAGE))
        _state.update { it.copy(isEditing = true) }
        return try {
            when (val authRequest = client.createEditFundingInstrumentAuthRequest(context, payPalCheckoutRequest)) {
                // There's no view left to return to.
                is PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch ->
                    if (activity.isFinishing || activity.isDestroyed) {
                        null
                    } else {
                        payPalLauncher.launch(activity, authRequest.paymentAuthRequest)
                    }
                is PayPalSavedPaymentMethodAuthRequest.Failure -> PayPalPendingRequest.Failure(authRequest.error)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            PayPalPendingRequest.Failure(e)
        } finally {
            _state.update { it.copy(isEditing = false) }
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
    ): PayPalSavedPaymentMethodReturnResult = when {
        completedReturns.contains(pendingRequest) -> PayPalSavedPaymentMethodReturnResult.COMPLETED
        state.value.isEditing -> PayPalSavedPaymentMethodReturnResult.NOT_TAKEN
        intent == null -> {
            pendingResults.add(PayPalResult.Failure(BraintreeException(NO_INTENT_MESSAGE)))
            PayPalSavedPaymentMethodReturnResult.TAKEN
        }
        else -> readReturn(pendingRequest, payPalLauncher, intent)
    }

    private fun readReturn(
        pendingRequest: String,
        payPalLauncher: PayPalLauncher,
        intent: Intent
    ): PayPalSavedPaymentMethodReturnResult {
        val authResult = try {
            payPalLauncher.restorePendingRequest(pendingRequest)
            payPalLauncher.handleReturnToApp(PayPalPendingRequest.Started(pendingRequest), intent)
        } catch (e: Exception) {
            return failReturn(pendingRequest, e)
        }
        return when (authResult) {
            is PayPalPaymentAuthResult.Success -> {
                // Recorded before tokenizing, which uses up the PayPal approval, so it's never tokenized twice.
                completedReturns.mark(pendingRequest)
                tokenize(authResult)
                PayPalSavedPaymentMethodReturnResult.COMPLETED
            }
            is PayPalPaymentAuthResult.Failure -> failReturn(pendingRequest, authResult.error)
            PayPalPaymentAuthResult.NoResult -> {
                pendingResults.add(PayPalResult.Cancel)
                PayPalSavedPaymentMethodReturnResult.TAKEN
            }
        }
    }

    private fun tokenize(authResult: PayPalPaymentAuthResult.Success) {
        _state.update { it.copy(isEditing = true) }
        viewModelScope.launch {
            val result = try {
                client.tokenize(authResult)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                PayPalResult.Failure(e)
            } finally {
                _state.update { it.copy(isEditing = false) }
            }
            when (result) {
                is PayPalResult.Success -> {
                    saveEdit(editedOrderId = result.nonce.paymentId)
                    loadBuyerUpdatedBillingAgreement()
                }
                // A cancel on the PayPal page doesn't use up the return, so the buyer can complete it later.
                PayPalResult.Cancel -> completedReturns.forget()
                is PayPalResult.Failure -> Unit
            }
            pendingResults.add(result)
        }
    }

    private fun failReturn(pendingRequest: String, error: Exception): PayPalSavedPaymentMethodReturnResult {
        completedReturns.mark(pendingRequest)
        pendingResults.add(PayPalResult.Failure(error))
        return PayPalSavedPaymentMethodReturnResult.COMPLETED
    }

    private fun saveEdit(editedOrderId: String?) {
        savedState[IS_EDIT_COMPLETED_KEY] = true
        savedState[EDITED_ORDER_ID_KEY] = editedOrderId
        // The Pay Later offer was quoted for the funding instrument from before the edit.
        _state.update {
            it.copy(isEditCompleted = true, creditMessaging = PayPalCreditMessagingDisplayState.Hidden)
        }
    }

    private fun loadBuyerDefaultBillingAgreement() {
        val merchantAccountId = inputs?.request?.merchantAccountId
        defaultFundingInstrumentJob?.cancel()
        defaultFundingInstrumentJob = viewModelScope.launch {
            val fundingInstrument = try {
                client.fetchPaymentMethod(
                    PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
                    null,
                    merchantAccountId
                ).toDisplayState()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // A failed refresh keeps what's shown; only a failed first load shows the error.
                if (state.value.fundingInstrument != PayPalSavedPaymentMethodDisplayState.Loading) return@launch
                e.toDisplayState()
            }
            // An edit that completed meanwhile wins.
            if (!state.value.isEditCompleted) _state.update { it.copy(fundingInstrument = fundingInstrument) }
        }
    }

    // If the picked funding instrument can't be loaded only the brand shows, since the old one is no longer charged.
    private fun loadBuyerUpdatedBillingAgreement() {
        val merchantAccountId = inputs?.request?.merchantAccountId
        updatedFundingInstrumentJob?.cancel()
        val orderId = savedState.get<String>(EDITED_ORDER_ID_KEY)
        if (orderId == null) {
            _state.update { it.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Error) }
            return
        }
        _state.update { it.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Loading) }
        updatedFundingInstrumentJob = viewModelScope.launch {
            val fundingInstrument = try {
                client.fetchPaymentMethod(
                    PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT,
                    orderId,
                    merchantAccountId
                ).toDisplayState()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                e.toDisplayState()
            }
            _state.update { it.copy(fundingInstrument = fundingInstrument) }
        }
    }

    private data class Inputs(val request: PayPalSavedPaymentMethodRequest, val showCreditMessaging: Boolean)

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
    }
}
