package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Builds the client for an authorization and its return URLs. */
internal typealias PayPalSavedPaymentMethodClientFactory =
    (authorization: String, appLinkReturnUrl: Uri, deepLinkFallbackUrlScheme: String?) -> PayPalSavedPaymentMethodClient

/**
 * State and edit flow shared by the XML and Compose views. Created for one authorization and its return URLs.
 * The state survives configuration changes, and the completed edit survives process death. Each result reaches the
 * merchant once: it's kept until a view has handled it, so it still arrives if the view is recreated while tokenizing
 * or while the result is being delivered. A result lost with the process is not delivered again, because its PayPal
 * return can't be completed twice; the edited funding instrument is still restored.
 *
 * The launcher, activity and intent are passed per call and never kept. Main thread only.
 */
@OptIn(ExperimentalBetaApi::class)
@Suppress("TooManyFunctions")
internal class PayPalSavedPaymentMethodViewModel(
    private val savedState: SavedStateHandle,
    authorization: String,
    private val appLinkReturnUrl: Uri,
    private val deepLinkFallbackUrlScheme: String?,
    createClient: PayPalSavedPaymentMethodClientFactory
) : ViewModel() {

    private val client = createClient(authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)

    private val mutableState = MutableStateFlow(
        PayPalSavedPaymentMethodUiState(
            isEditCompleted = savedState[IS_EDIT_COMPLETED_KEY] ?: false,
            editedOrderId = savedState[EDITED_ORDER_ID_KEY]
        )
    )
    val state: StateFlow<PayPalSavedPaymentMethodUiState> = mutableState.asStateFlow()

    // Results not yet handled by a view, oldest first. A result sent while no view collects waits for the next one.
    private val pendingResults = MutableStateFlow<List<PayPalResult>>(emptyList())

    /**
     * Delivers each result once. A result is removed only after the collector has handled it, or has stopped after
     * taking it (for example with `first()`); if the collector is cancelled mid-delivery, for example on rotation, the
     * next one receives it.
     */
    val results: Flow<PayPalResult> = flow {
        pendingResults.collect { pending ->
            val result = pending.firstOrNull() ?: return@collect
            try {
                emit(result)
            } finally {
                // Inactive only when cancelled, so the result wasn't handled.
                if (currentCoroutineContext().isActive) {
                    pendingResults.update { if (it.firstOrNull() === result) it.drop(1) else it }
                }
            }
        }
    }

    private var request: PayPalSavedPaymentMethodRequest? = null
    private var showCreditMessaging = true
    private var updatedFundingInstrumentJob: Job? = null
    private var creditMessagingJob: Job? = null

    // The XML view showing this view model, so two views with the same id can't share it.
    private var attachedViewId: String? = null

    /** Whether another view is showing this view model. */
    fun isAttachedToAnotherView(viewInstanceId: String): Boolean =
        attachedViewId.let { it != null && it != viewInstanceId }

    /** Records that the view shows this view model. Returns `false` if another view already does. */
    fun attachView(viewInstanceId: String): Boolean {
        if (isAttachedToAnotherView(viewInstanceId)) return false
        attachedViewId = viewInstanceId
        return true
    }

    /** Records that the view no longer shows this view model. */
    fun detachView(viewInstanceId: String) {
        if (attachedViewId == viewInstanceId) attachedViewId = null
    }

    /**
     * Loads the default funding instrument and, if shown, the Pay Later message. Only the first call loads. After an
     * edit restored from [savedState], loads the funding instrument the buyer picked and no message.
     */
    fun load(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        if (this.request != null) return
        this.request = request
        this.showCreditMessaging = showCreditMessaging
        if (state.value.isEditCompleted) loadBuyerUpdatedBillingAgreement() else loadBuyerDefaultBillingAgreement()
        startCreditFetch()
    }

    /**
     * Reloads the Pay Later message when the request changes or it's turned back on, and hides it when turned off.
     * Does nothing before [load].
     */
    fun onRequestChanged(request: PayPalSavedPaymentMethodRequest, showCreditMessaging: Boolean) {
        val previous = this.request ?: return
        val showedCreditMessaging = this.showCreditMessaging
        this.request = request
        this.showCreditMessaging = showCreditMessaging
        if (request != previous || showCreditMessaging != showedCreditMessaging) startCreditFetch()
    }

    /**
     * Opens the PayPal paysheet. Runs in the caller's scope because it launches from [context]'s activity. Returns the
     * pending request to keep, a failure to report, or `null` if an edit step is already running or the activity
     * closed while the auth request was created.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun startEdit(
        context: Context,
        payPalLauncher: PayPalLauncher,
        payPalCheckoutRequest: PayPalCheckoutRequest
    ): PayPalPendingRequest? {
        // One edit step at a time.
        if (state.value.isEditing) return null
        mutableState.update { it.copy(isEditing = true) }
        // TODO: send the edit flow analytics events once the catalog is approved.
        return try {
            val activity = context.findActivity() as? ComponentActivity
                ?: return PayPalPendingRequest.Failure(BraintreeException(NO_COMPONENT_ACTIVITY_MESSAGE))
            when (val authRequest = client.createEditFundingInstrumentAuthRequest(context, payPalCheckoutRequest)) {
                is PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch ->
                    // The activity may have started closing while the auth request was created; no view is left to
                    // return to.
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
            mutableState.update { it.copy(isEditing = false) }
        }
    }

    /**
     * Completes the return from PayPal in [intent], tokenizes and delivers the result through [results]. A return
     * with no result is a [PayPalResult.Cancel]. Runs in the view model's scope, so recreating the view mid-tokenize
     * neither cancels nor repeats it. A [pendingRequest] that was already completed is ignored and its return URL is
     * removed from [intent]; a cancel doesn't count, so the buyer can still return with the same pending request.
     * After a success the funding instrument the buyer picked is loaded, and the Pay Later message is not.
     *
     * Returns `false` if the return was dropped because an edit step is running, so the caller keeps [pendingRequest]
     * and passes it again. Otherwise returns `true`: the return is being handled or was already completed, and the
     * caller can clear its stored copy. A result lost to process death after that is not delivered again.
     */
    fun handleReturnToApp(pendingRequest: String, payPalLauncher: PayPalLauncher, intent: Intent?): Boolean {
        if (pendingRequest == savedState[COMPLETED_PENDING_REQUEST_KEY]) {
            // The restored intent still carries its return URL, which a later edit mustn't read.
            intent?.clearReturnUrl()
            return true
        }
        // One edit step at a time.
        if (state.value.isEditing) return false
        mutableState.update { it.copy(isEditing = true) }
        viewModelScope.launch {
            val result = try {
                finishReturn(pendingRequest, payPalLauncher, intent)
            } finally {
                mutableState.update { it.copy(isEditing = false) }
            }
            if (result is PayPalResult.Success) {
                creditMessagingJob?.cancel()
                saveEdit(editedOrderId = result.nonce.paymentId)
                loadBuyerUpdatedBillingAgreement()
            }
            pendingResults.update { it + result }
        }
        return true
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun finishReturn(
        pendingRequest: String,
        payPalLauncher: PayPalLauncher,
        intent: Intent?
    ): PayPalResult {
        // TODO: send the edit flow analytics events once the catalog is approved.
        if (intent == null) return PayPalResult.Failure(BraintreeException(NO_INTENT_MESSAGE))
        return try {
            payPalLauncher.restorePendingRequest(pendingRequest)
            val started = PayPalPendingRequest.Started(pendingRequest)
            when (val authResult = payPalLauncher.handleReturnToApp(started, intent)) {
                is PayPalPaymentAuthResult.Success -> {
                    // Recorded before tokenizing, which uses up the PayPal approval; a return interrupted after this
                    // point is not repeated.
                    markCompleted(pendingRequest)
                    client.tokenize(authResult).also {
                        intent.clearReturnUrl()
                        // A cancel on the PayPal page doesn't count, so the buyer can still return with this request.
                        if (it is PayPalResult.Cancel) savedState.remove<String>(COMPLETED_PENDING_REQUEST_KEY)
                    }
                }
                // Cleared too, so a later edit doesn't read the same return URL.
                is PayPalPaymentAuthResult.Failure -> {
                    markCompleted(pendingRequest)
                    PayPalResult.Failure(authResult.error).also { intent.clearReturnUrl() }
                }
                // Not recorded for a cancel, so the buyer can still return with the same pending request.
                PayPalPaymentAuthResult.NoResult -> PayPalResult.Cancel
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            markCompleted(pendingRequest)
            // Cleared here as well.
            intent.clearReturnUrl()
            PayPalResult.Failure(e)
        }
    }

    private fun markCompleted(pendingRequest: String) {
        savedState[COMPLETED_PENDING_REQUEST_KEY] = pendingRequest
    }

    private fun saveEdit(editedOrderId: String?) {
        savedState[IS_EDIT_COMPLETED_KEY] = true
        savedState[EDITED_ORDER_ID_KEY] = editedOrderId
        // The Pay Later offer was quoted for the funding instrument from before the edit.
        mutableState.update {
            it.copy(
                isEditCompleted = true,
                editedOrderId = editedOrderId,
                creditMessaging = PayPalCreditMessagingDisplayState.Hidden
            )
        }
    }

    // The funding instrument PayPal charges unless the buyer changes it.
    private fun loadBuyerDefaultBillingAgreement() {
        val merchantAccountId = request?.merchantAccountId
        viewModelScope.launch {
            val fundingInstrument = fetchFundingInstrument(
                PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
                null,
                merchantAccountId
            )
            // An edit completed meanwhile; its funding instrument wins.
            if (state.value.isEditCompleted) return@launch
            mutableState.update { it.copy(fundingInstrument = fundingInstrument) }
        }
    }

    // The funding instrument the buyer picked. If it can't be loaded only the PayPal logo and label show, since the
    // earlier one is no longer charged.
    private fun loadBuyerUpdatedBillingAgreement() {
        val merchantAccountId = request?.merchantAccountId
        updatedFundingInstrumentJob?.cancel()
        val orderId = state.value.editedOrderId
        if (orderId == null) {
            mutableState.update { it.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Error) }
            return
        }
        mutableState.update { it.copy(fundingInstrument = PayPalSavedPaymentMethodDisplayState.Loading) }
        updatedFundingInstrumentJob = viewModelScope.launch {
            val fundingInstrument = fetchFundingInstrument(
                PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT,
                orderId,
                merchantAccountId
            )
            mutableState.update { it.copy(fundingInstrument = fundingInstrument) }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchFundingInstrument(
        fundingInstrumentType: PayPalFundingInstrumentFetchType,
        orderId: String?,
        merchantAccountId: String?
    ): PayPalSavedPaymentMethodDisplayState = try {
        client.fetchPaymentMethod(fundingInstrumentType, orderId, merchantAccountId).toDisplayState()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        e.toDisplayState()
    }

    // Replaces the fetch in flight, so an older amount's message can't land last.
    @Suppress("TooGenericExceptionCaught")
    private fun startCreditFetch() {
        val request = request ?: return
        creditMessagingJob?.cancel()
        // Hidden when turned off, and after an edit since the offer no longer applies.
        if (!showCreditMessaging || state.value.isEditCompleted) {
            mutableState.update { it.copy(creditMessaging = PayPalCreditMessagingDisplayState.Hidden) }
        } else {
            creditMessagingJob = viewModelScope.launch {
                val creditMessaging = try {
                    client.fetchCreditPresentmentMessages(request.amount, request.currencyCode).toDisplayState()
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // A failure only hides the row.
                    PayPalCreditMessagingDisplayState.Hidden
                }
                mutableState.update { it.copy(creditMessaging = creditMessaging) }
            }
        }
    }

    private fun Intent.clearReturnUrl() {
        if (data?.isReturnUrl() == true) data = null
    }

    /** Whether this is the PayPal return URL, matched as the browser switch does. */
    private fun Uri.isReturnUrl(): Boolean {
        val deepLinkScheme = deepLinkFallbackUrlScheme
        val isDeepLinkReturn = deepLinkScheme != null && scheme.equals(deepLinkScheme, ignoreCase = true)
        val isAppLinkReturn = scheme.equals(appLinkReturnUrl.scheme, ignoreCase = true) &&
            host.equals(appLinkReturnUrl.host, ignoreCase = true)
        return isDeepLinkReturn || isAppLinkReturn
    }

    /** Creates the view model for these values, restoring the completed edit from saved state. */
    class Factory(
        private val authorization: String,
        private val appLinkReturnUrl: Uri,
        private val deepLinkFallbackUrlScheme: String?,
        private val createClient: PayPalSavedPaymentMethodClientFactory
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedState = try {
                extras.createSavedStateHandle()
            } catch (ignored: IllegalArgumentException) {
                // No saved state owner, so nothing survives process death.
                SavedStateHandle()
            }
            return PayPalSavedPaymentMethodViewModel(
                savedState,
                authorization,
                appLinkReturnUrl,
                deepLinkFallbackUrlScheme,
                createClient
            ) as T
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

/** Builds the real client. Uses the application context so no activity is retained. */
internal fun defaultClientFactory(context: Context): PayPalSavedPaymentMethodClientFactory {
    val applicationContext = context.applicationContext
    return { authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme ->
        PayPalSavedPaymentMethodClient(applicationContext, authorization, appLinkReturnUrl, deepLinkFallbackUrlScheme)
    }
}
