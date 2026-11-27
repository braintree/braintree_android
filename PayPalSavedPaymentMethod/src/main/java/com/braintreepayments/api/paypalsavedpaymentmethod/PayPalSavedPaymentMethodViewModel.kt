package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.withStarted
import androidx.lifecycle.viewmodel.CreationExtras
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodAnalytics.RENDER_TRIGGER_FI_FROM_APPROVED_CHECKOUT
import com.braintreepayments.api.paypalsavedpaymentmethod.PayPalSavedPaymentMethodAnalytics.RENDER_TRIGGER_STICKY_FI
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalSavedPaymentMethodUiState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.leavesRoomForCreditMessaging
import com.braintreepayments.api.paypalsavedpaymentmethod.state.toDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.state.visibleWith
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodViewStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

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
@Suppress("TooGenericExceptionCaught", "TooManyFunctions")
internal class PayPalSavedPaymentMethodViewModel(
    private val savedState: SavedStateHandle,
    private val client: PayPalSavedPaymentMethodClient
) : ViewModel() {

    // A completed edit is also kept in saved state, written together with the state in saveEdit.
    private val _state = MutableStateFlow(
        PayPalSavedPaymentMethodUiState(isEditCompleted = savedState[IS_EDIT_COMPLETED_KEY] ?: false)
    )
    val state: StateFlow<PayPalSavedPaymentMethodUiState> = _state.asStateFlow()

    /** Keys the PayPal launcher's activity result: unique to this view model, and kept across process death. */
    val resultKey: String = savedState.get<String>(RESULT_KEY_KEY)
        ?: UUID.randomUUID().toString().also { savedState[RESULT_KEY_KEY] = it }

    private val pendingResults = PendingResults()
    private val completedReturns = CompletedReturns(savedState)
    private val creditMessagingLoader =
        CreditMessagingLoader(client, _state, viewModelScope, ::onCreditMessagingSettled)

    /** Delivers each result at least once: a result whose collector is cancelled goes to the next collector. */
    val results: Flow<PayPalResult> = pendingResults.results

    // What the views asked for; null until the first load.
    private var inputs: Inputs? = null
    private var defaultFundingInstrumentJob: Job? = null
    private var isDefaultFundingInstrumentLoaded = false
    private var updatedFundingInstrumentJob: Job? = null

    // A funding instrument shown since the last render, so each is reported once, not on every re-render.
    private var isFundingInstrumentUnreported = false

    private var reportedCreditMessage: PayPalCreditMessagingContent? = null

    // Why the latest Pay Later message can't be shown, until reported.
    private var unreportedCreditMessagingFailure: Exception? = null

    /**
     * Loads the default funding instrument and, if enabled, the Pay Later message. Called each time a view binds, so a
     * failed load is retried; one that succeeded or is still running isn't fetched again, so a configuration change or
     * returning to the screen makes no new requests. After an edit the funding instrument isn't refetched, since the
     * API still returns the one from before the edit; only an edit restored from [savedState] loads it once.
     */
    fun load(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        val isFirstLoad = inputs == null
        // Once per button_session_id, so a view model restored after process death doesn't send it again.
        if (savedState.get<Boolean>(RENDER_STARTED_KEY) != true) {
            savedState[RENDER_STARTED_KEY] = true
            client.sendAnalyticsEvent(PayPalSavedPaymentMethodAnalytics.RENDER_STARTED)
        }
        inputs = Inputs(request, showCreditMessaging)
        creditMessagingLoader.fetchIfNotShown(request, showCreditMessaging)
        if (!state.value.isEditCompleted) {
            if (!isDefaultFundingInstrumentLoaded && defaultFundingInstrumentJob?.isActive != true) {
                loadBuyerDefaultBillingAgreement()
            }
        } else if (isFirstLoad) {
            loadBuyerUpdatedBillingAgreement()
        }
    }

    /**
     * Reloads or hides the Pay Later message when the request or its setting changes, and reloads the funding
     * instrument when the merchant account changes, since it's resolved against that account. Does nothing before
     * [load].
     */
    fun onRequestChanged(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        val previous = inputs ?: return
        val changed = Inputs(request, showCreditMessaging)
        if (previous == changed) return
        inputs = changed
        creditMessagingLoader.fetch(request, showCreditMessaging)
        if (request.merchantAccountId != previous.request.merchantAccountId && !state.value.isEditCompleted) {
            isDefaultFundingInstrumentLoaded = false
            _state.update { it.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Loading) }
            loadBuyerDefaultBillingAgreement()
        }
    }

    /**
     * Opens the PayPal paysheet from [context]'s activity. Returns the pending request to keep, a failure to report,
     * or `null` if an edit step is already running or the activity closed meanwhile. The paysheet opens, and the
     * result returns, only while [hostLifecycle] is started, so a buyer who leaves during setup isn't sent to PayPal
     * from the background; if the host is destroyed first, this is cancelled.
     */
    suspend fun startEdit(
        context: Context,
        payPalLauncher: PayPalLauncher,
        payPalCheckoutRequest: PayPalCheckoutRequest,
        hostLifecycle: Lifecycle
    ): PayPalPendingRequest? {
        if (state.value.isEditing) return null
        // Queued, so it goes out in the same batch as paypal:tokenize:started and ahead of it. Sent on its own, the
        // two separate requests can reach FPTI in either order.
        client.sendAnalyticsEvent(
            PayPalSavedPaymentMethodAnalytics.FUNDING_INSTRUMENT_EDIT_SELECTED,
            shownPaymentMethod().savedMethodParams(),
            sendImmediately = false
        )
        val activity = context.findActivity() as? ComponentActivity
            ?: return PayPalPendingRequest.Failure(BraintreeException(NO_COMPONENT_ACTIVITY_MESSAGE))
        _state.update { it.copy(isEditing = true) }
        return try {
            val authRequest = client.createEditFundingInstrumentAuthRequest(context, payPalCheckoutRequest)
            hostLifecycle.withStarted {
                when (authRequest) {
                    // There's no view left to return to.
                    is PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch ->
                        if (activity.isFinishing || activity.isDestroyed) {
                            null
                        } else {
                            payPalLauncher.launch(activity, authRequest.paymentAuthRequest)
                        }
                    is PayPalSavedPaymentMethodAuthRequest.Failure -> PayPalPendingRequest.Failure(authRequest.error)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            hostLifecycle.withStarted { PayPalPendingRequest.Failure(e) }
        } finally {
            _state.update { it.copy(isEditing = false) }
        }
    }

    /**
     * Reads the PayPal return in [intent] and delivers the result through [results]. Tokenizing runs in the view
     * model's scope, so recreating the view doesn't cancel or repeat it. A return with no result, including the buyer
     * closing the paysheet, is a [PayPalResult.Cancel], as in `PayPalButton`. After a success the funding instrument
     * the buyer picked is loaded. A return already completed, including before process death, isn't completed again.
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
            payPalLauncher.handleReturnToApp(
                PayPalPendingRequest.Started(pendingRequest),
                intent,
                isEditFiFlow = true,
                buttonSessionId = client.buttonSessionId
            )
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

    /** Reports a tap on the Pay Later message's "Learn more" link. */
    fun onLearnMoreSelected() {
        client.sendAnalyticsEvent(
            PayPalSavedPaymentMethodAnalytics.CREDIT_MESSAGING_LEARN_MORE_SELECTED,
            AnalyticsEventParams(presentmentMessageType = visibleCreditMessage()?.messageType)
        )
    }

    /**
     * Called by the views each time they render [state]. Reports a newly shown funding instrument and Pay Later message
     * once each, so re-rendering, for example after rotation, sends nothing new. Loading isn't a render.
     */
    fun onRendered(style: PayPalSavedPaymentMethodViewStyle) {
        val current = state.value
        if (current.fundingInstrument == PayPalSavedPaymentMethodDisplayState.Loading) return
        if (isFundingInstrumentUnreported) {
            isFundingInstrumentUnreported = false
            sendFundingInstrumentRendered(current, style)
        }
        sendCreditMessagingFailedIfNeeded()

        val creditMessage = visibleCreditMessage()
        if (creditMessage != reportedCreditMessage) {
            reportedCreditMessage = creditMessage
            creditMessage?.let {
                client.sendAnalyticsEvent(
                    PayPalSavedPaymentMethodAnalytics.CREDIT_MESSAGING_PRESENTED,
                    AnalyticsEventParams(presentmentMessageType = it.messageType)
                )
            }
        }
    }

    private fun sendFundingInstrumentRendered(
        current: PayPalSavedPaymentMethodUiState,
        style: PayPalSavedPaymentMethodViewStyle
    ) {
        val brandOrHidden = if (style.showPayPalLogo || style.showPayPalLabel) {
            PayPalSavedPaymentMethodAnalytics.SHOWN_CONTENT_BRAND_ONLY
        } else {
            PayPalSavedPaymentMethodAnalytics.SHOWN_CONTENT_HIDDEN
        }
        val (shownContent, renderedEditTag) = when (val fundingInstrument = current.fundingInstrument) {
            is PayPalSavedPaymentMethodDisplayState.Content ->
                PayPalSavedPaymentMethodAnalytics.SHOWN_CONTENT_FUNDING_INSTRUMENT to true
            is PayPalSavedPaymentMethodDisplayState.NoFundingInstrument ->
                PayPalSavedPaymentMethodAnalytics.SHOWN_CONTENT_EMAIL_ONLY to fundingInstrument.isEditable
            PayPalSavedPaymentMethodDisplayState.Loading,
            PayPalSavedPaymentMethodDisplayState.NoNetwork,
            PayPalSavedPaymentMethodDisplayState.Error,
            PayPalSavedPaymentMethodDisplayState.Hidden -> brandOrHidden to false
        }
        val renderTrigger =
            if (current.isEditCompleted) RENDER_TRIGGER_FI_FROM_APPROVED_CHECKOUT else RENDER_TRIGGER_STICKY_FI
        client.sendAnalyticsEvent(
            PayPalSavedPaymentMethodAnalytics.RENDER_PRESENTED,
            AnalyticsEventParams(
                renderTrigger = renderTrigger,
                shownContent = shownContent,
                renderedLogo = style.showPayPalLogo,
                renderedLabel = style.showPayPalLabel,
                renderedEditTag = renderedEditTag,
                renderedMessageSlot = style.showPayPalCreditMessaging && current.allowsCreditMessaging()
            )
        )
        val paymentMethod = shownPaymentMethod() ?: return
        client.sendAnalyticsEvent(
            PayPalSavedPaymentMethodAnalytics.FUNDING_INSTRUMENT_PRESENTED,
            paymentMethod.savedMethodParams().copy(
                contextId = savedState[EDITED_ORDER_ID_KEY],
                renderTrigger = renderTrigger
            )
        )
    }

    // Holds why the Pay Later message can't be shown, or null when it can, until the funding instrument allows it.
    private fun onCreditMessagingSettled(failure: Exception?) {
        unreportedCreditMessagingFailure = failure
        sendCreditMessagingFailedIfNeeded()
    }

    private fun sendCreditMessagingFailedIfNeeded() {
        val current = state.value
        if (!current.allowsCreditMessaging()) return
        val failure = unreportedCreditMessagingFailure ?: return
        unreportedCreditMessagingFailure = null
        client.sendAnalyticsEvent(
            PayPalSavedPaymentMethodAnalytics.CREDIT_MESSAGING_FAILED,
            AnalyticsEventParams(errorDescription = failure.analyticsDescription())
        )
    }

    private fun sendFundingInstrumentFailed(error: Exception, renderTrigger: String, orderId: String?) {
        client.sendAnalyticsEvent(
            PayPalSavedPaymentMethodAnalytics.FUNDING_INSTRUMENT_FAILED,
            AnalyticsEventParams(
                contextId = orderId,
                errorDescription = error.analyticsDescription(),
                renderTrigger = renderTrigger
            )
        )
    }

    private fun shownPaymentMethod(): PayPalSavedPaymentMethod? =
        (state.value.fundingInstrument as? PayPalSavedPaymentMethodDisplayState.Content)?.paymentMethod

    private fun visibleCreditMessage(): PayPalCreditMessagingContent? {
        val current = state.value
        val creditMessaging = current.creditMessaging.visibleWith(current.fundingInstrument, current.isEditCompleted)
        return (creditMessaging as? PayPalCreditMessagingDisplayState.Content)?.content
    }

    // A changed funding instrument is reported on the next render.
    private fun showFundingInstrument(fundingInstrument: PayPalSavedPaymentMethodDisplayState) {
        if (fundingInstrument != state.value.fundingInstrument) isFundingInstrumentUnreported = true
        _state.update { it.copy(fundingInstrument = fundingInstrument) }
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
            var failure: Exception? = null
            val fundingInstrument = try {
                client.fetchPaymentMethod(
                    PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
                    null,
                    merchantAccountId
                ).toDisplayState().also { isDefaultFundingInstrumentLoaded = true }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // A failed retry keeps the error shown; only a failed first load sets it.
                if (state.value.fundingInstrument != PayPalSavedPaymentMethodDisplayState.Loading) return@launch
                failure = e
                e.toDisplayState()
            }
            // An edit that completed meanwhile wins.
            if (state.value.isEditCompleted) return@launch
            showFundingInstrument(fundingInstrument)
            failure?.let { sendFundingInstrumentFailed(it, RENDER_TRIGGER_STICKY_FI, orderId = null) }
        }
    }

    // If the picked funding instrument can't be loaded only the brand shows, since the old one is no longer charged.
    private fun loadBuyerUpdatedBillingAgreement() {
        val merchantAccountId = inputs?.request?.merchantAccountId
        updatedFundingInstrumentJob?.cancel()
        val orderId = savedState.get<String>(EDITED_ORDER_ID_KEY)
        if (orderId == null) {
            showFundingInstrument(PayPalSavedPaymentMethodDisplayState.Error)
            sendFundingInstrumentFailed(
                PayPalSavedPaymentMethodException.MissingOrderId(),
                RENDER_TRIGGER_FI_FROM_APPROVED_CHECKOUT,
                orderId = null
            )
            return
        }
        showFundingInstrument(PayPalSavedPaymentMethodDisplayState.Loading)
        updatedFundingInstrumentJob = viewModelScope.launch {
            var failure: Exception? = null
            val fundingInstrument = try {
                client.fetchPaymentMethod(
                    PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT,
                    orderId,
                    merchantAccountId
                ).toDisplayState()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                failure = e
                e.toDisplayState()
            }
            showFundingInstrument(fundingInstrument)
            failure?.let { sendFundingInstrumentFailed(it, RENDER_TRIGGER_FI_FROM_APPROVED_CHECKOUT, orderId) }
        }
    }

    private data class Inputs(val request: PayPalSavedPaymentMethodRequest, val showCreditMessaging: Boolean)

    /**
     * Creates the view model with the client from [createClient], restoring the completed edit and the component's
     * `button_session_id` from saved state, so the ID survives process death and is only new for a new component.
     */
    class Factory(
        private val createClient: (buttonSessionId: String) -> PayPalSavedPaymentMethodClient
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedState = try {
                extras.createSavedStateHandle()
            } catch (ignored: IllegalArgumentException) {
                // No saved state owner, so nothing survives process death.
                SavedStateHandle()
            }
            val buttonSessionId = savedState[BUTTON_SESSION_ID_KEY]
                ?: newButtonSessionId().also { savedState[BUTTON_SESSION_ID_KEY] = it }
            return PayPalSavedPaymentMethodViewModel(savedState, createClient(buttonSessionId)) as T
        }
    }

    companion object {
        const val NO_COMPONENT_ACTIVITY_MESSAGE =
            "PayPalSavedPaymentMethodView must be hosted in a ComponentActivity."
        const val NO_INTENT_MESSAGE = "Unable to read the return from PayPal: no intent was given."
        private const val IS_EDIT_COMPLETED_KEY = "is_edit_completed"
        private const val EDITED_ORDER_ID_KEY = "edited_order_id"
        private const val RESULT_KEY_KEY = "result_key"
        private const val BUTTON_SESSION_ID_KEY = "button_session_id"
        private const val RENDER_STARTED_KEY = "render_started"
    }
}

// Whether the funding instrument leaves room for the Pay Later row, whatever the message's own state.
private fun PayPalSavedPaymentMethodUiState.allowsCreditMessaging(): Boolean =
    !isEditCompleted && fundingInstrument.leavesRoomForCreditMessaging

private fun PayPalSavedPaymentMethod?.savedMethodParams() = AnalyticsEventParams(
    savedMethodType = this?.rawType,
    savedMethodId = this?.id,
    savedMethodBrand = this?.cardBrand
)

// An exception without a message is described by its type, so the failed event always says why.
private fun Exception.analyticsDescription(): String = message ?: javaClass.simpleName
