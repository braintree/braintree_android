package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.toDisplayState
import kotlinx.coroutines.CancellationException

/**
 * Loads what the component renders and runs the edit flow. Shared by the XML and Compose views, which only hold the
 * resulting state and render it. Every failure is mapped to a display state or a [PayPalResult]; a
 * `CancellationException` is always re-thrown.
 */
internal class PayPalSavedPaymentMethodController(
    private val savedPaymentMethodClient: PayPalSavedPaymentMethodClient,
    private val appLinkReturnUrl: Uri,
    private val deepLinkFallbackUrlScheme: String?
) {

    /** Loads the buyer's default funding instrument. */
    suspend fun loadFundingInstrument(merchantAccountId: String?): PayPalSavedPaymentMethodDisplayState =
        fetchFundingInstrument(
            fundingInstrumentType = PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
            orderId = null,
            merchantAccountId = merchantAccountId
        )

    /**
     * Loads the funding instrument the buyer picked in the edit, for the approved order [orderId]. The one shown before
     * the edit is no longer the one that will be charged, so the component is hidden if the picked one can't be shown.
     */
    suspend fun loadEditedFundingInstrument(
        orderId: String?,
        merchantAccountId: String?
    ): PayPalSavedPaymentMethodDisplayState {
        if (orderId == null) return PayPalSavedPaymentMethodDisplayState.Hidden
        val state = fetchFundingInstrument(
            fundingInstrumentType = PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT,
            orderId = orderId,
            merchantAccountId = merchantAccountId
        )
        return if (state.showsFundingInstrumentRow) state else PayPalSavedPaymentMethodDisplayState.Hidden
    }

    /** Loads the Pay Later message for the amount and currency in [request]. */
    @OptIn(ExperimentalBetaApi::class)
    @Suppress("TooGenericExceptionCaught")
    suspend fun loadCreditMessaging(request: PayPalSavedPaymentMethodRequest): PayPalCreditMessagingDisplayState =
        try {
            savedPaymentMethodClient.fetchCreditPresentmentMessages(request.amount, request.currencyCode)
                .toDisplayState()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // The message is additive: a failure only hides its row.
            PayPalCreditMessagingDisplayState.Hidden
        }

    /**
     * Opens the PayPal paysheet for the edit flow. Returns the [PayPalPendingRequest.Started] the caller must keep to
     * complete the return, or a [PayPalPendingRequest.Failure] to report.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun startEditFlow(
        context: Context,
        payPalLauncher: PayPalLauncher,
        payPalCheckoutRequest: PayPalCheckoutRequest
    ): PayPalPendingRequest {
        // TODO: send the edit flow analytics events once the catalog is approved.
        try {
            val activity = context.findActivity() as? ComponentActivity
                ?: return PayPalPendingRequest.Failure(BraintreeException(NO_COMPONENT_ACTIVITY_MESSAGE))
            return when (
                val authRequest =
                    savedPaymentMethodClient.createEditFundingInstrumentAuthRequest(context, payPalCheckoutRequest)
            ) {
                is PayPalSavedPaymentMethodAuthRequest.ReadyToLaunch ->
                    payPalLauncher.launch(activity, authRequest.paymentAuthRequest)
                is PayPalSavedPaymentMethodAuthRequest.Failure -> PayPalPendingRequest.Failure(authRequest.error)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return PayPalPendingRequest.Failure(e)
        }
    }

    /**
     * Completes the return from PayPal in [intent] and tokenizes the PayPal account. A return without a result means
     * the buyer left the paysheet, so it is reported as [PayPalResult.Cancel]. After a success or a failure, the PayPal
     * return URL is removed from [intent] so it isn't handled twice.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun completeEditFlow(
        pendingRequest: String,
        payPalLauncher: PayPalLauncher,
        intent: Intent?
    ): PayPalResult {
        // TODO: send the edit flow analytics events once the catalog is approved.
        try {
            if (intent == null) return PayPalResult.Failure(BraintreeException(NO_INTENT_MESSAGE))

            payPalLauncher.restorePendingRequest(pendingRequest)
            val authResult = payPalLauncher.handleReturnToApp(PayPalPendingRequest.Started(pendingRequest), intent)
            return when (authResult) {
                is PayPalPaymentAuthResult.Success -> savedPaymentMethodClient.tokenize(authResult).also {
                    // Cleared only after tokenizing, so a return interrupted by recreation is retried; left in place,
                    // the next edit would read the same return URL again.
                    intent.clearReturnUrl()
                }
                is PayPalPaymentAuthResult.Failure -> PayPalResult.Failure(authResult.error).also {
                    // A failed return is consumed too; left in place, a later edit could read the same return URL.
                    intent.clearReturnUrl()
                }
                PayPalPaymentAuthResult.NoResult -> PayPalResult.Cancel
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return PayPalResult.Failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchFundingInstrument(
        fundingInstrumentType: PayPalFundingInstrumentFetchType,
        orderId: String?,
        merchantAccountId: String?
    ): PayPalSavedPaymentMethodDisplayState = try {
        savedPaymentMethodClient.fetchPaymentMethod(fundingInstrumentType, orderId, merchantAccountId).toDisplayState()
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        e.toDisplayState()
    }

    private fun Intent.clearReturnUrl() {
        if (data?.isReturnUrl() == true) data = null
    }

    /**
     * Whether this is the return URL from PayPal, matched the same way as the browser switch, so the merchant's own
     * deep links are left alone.
     */
    private fun Uri.isReturnUrl(): Boolean {
        val isDeepLinkReturn =
            deepLinkFallbackUrlScheme != null && scheme.equals(deepLinkFallbackUrlScheme, ignoreCase = true)
        val isAppLinkReturn = scheme.equals(appLinkReturnUrl.scheme, ignoreCase = true) &&
            host.equals(appLinkReturnUrl.host, ignoreCase = true)
        return isDeepLinkReturn || isAppLinkReturn
    }

    companion object {
        const val NO_COMPONENT_ACTIVITY_MESSAGE =
            "PayPalSavedPaymentMethodView must be hosted in a ComponentActivity."
        const val NO_INTENT_MESSAGE = "Unable to read the return from PayPal: no intent was given."
    }
}
