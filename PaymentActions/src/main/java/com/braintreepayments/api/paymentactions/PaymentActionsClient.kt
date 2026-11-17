package com.braintreepayments.api.paymentactions

import android.content.Context
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Callback for receiving a [PaymentActionResult] from a [PaymentActionsClient] operation.
 */
fun interface PaymentActionCallback {
    fun onResult(result: PaymentActionResult)
}

/**
 * Used to submit and drive a payment method through the Payment Actions flow.
 *
 * Once a [PaymentActionRequest] has been submitted, consumers should utilize [handleNextAction] to
 * drive the Payment Action through to completion. The basic interaction loop is:
 * 1. Submit a [PaymentActionRequest] to [submitForPaymentAction].
 * 2. Read [PaymentActionResult] and perform any required actions.
 * 3. Call [handleNextAction] after required actions are complete.
 * 4. Goto 2 unless a terminal [PaymentActionResult] is received.
 *
 */
class PaymentActionsClient internal constructor(
    private val braintreeClient: BraintreeClient,
    threeDSecureProvider: ThreeDSecureProvider,
    private val service: PaymentActionsService = PaymentActionsService(braintreeClient),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher),
) {

    private val nextActionHandler = NextActionHandler(service, threeDSecureProvider)

    /**
     * Tracks whether a [submitForPaymentAction] or [handleNextAction] call is already in flight on
     * this client instance. Shared by both, since both drive the same Payment Action.
     */
    private val isHandlingNextAction = AtomicBoolean(false)

    /**
     * Initializes a new [PaymentActionsClient] instance.
     *
     * @param context an Android [Context]
     * @param authorization a Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        context.applicationContext,
        BraintreeClient(context, authorization),
    )

    // BraintreeClient doesn't expose its context, so the 3D Secure provider is built here, where
    // both the application context and the BraintreeClient are in hand.
    private constructor(applicationContext: Context, braintreeClient: BraintreeClient) : this(
        braintreeClient,
        CardinalThreeDSecureProvider(applicationContext, braintreeClient),
    )

    /**
     * Submits a [PaymentActionRequest] to initiate a payment action flow.
     *
     * On success the [PaymentActionCallback.onResult] method will be invoked with a
     * [PaymentActionResult] variant describing the next required step.
     *
     * On failure the [PaymentActionCallback.onResult] method will be invoked with a
     * [PaymentActionResult.Failure] containing an exception.
     *
     * @param request  [PaymentActionRequest]
     * @param callback [PaymentActionCallback]
     */
    fun submitForPaymentAction(request: PaymentActionRequest, callback: PaymentActionCallback) =
        coroutineScope.launch { callback.onResult(submitForPaymentAction(request)) }

    /**
     * Submits a [PaymentActionRequest] to initiate a payment action flow.
     *
     * On success a [PaymentActionResult] variant describing the next required step is returned.
     *
     * On failure [PaymentActionResult.Failure] containing an exception is returned.
     *
     * If the Payment Action is still PROCESSING after the SDK's bounded retries,
     * [PaymentActionResult.Processing] is returned. Wait briefly, then call [handleNextAction] to
     * continue.
     *
     * @param request [PaymentActionRequest]
     * @return [PaymentActionResult], or a [PaymentActionResult.Failure] with a [BraintreeException]
     *   if called while a previous [submitForPaymentAction] or [handleNextAction] call on this
     *   client hasn't completed.
     */
    suspend fun submitForPaymentAction(request: PaymentActionRequest): PaymentActionResult {
        if (!isHandlingNextAction.compareAndSet(false, true)) {
            return PaymentActionResult.Failure(BraintreeException(SUBMIT_FOR_PAYMENT_ACTION_IN_PROGRESS_MESSAGE))
        }
        val result = try {
            braintreeClient.sendAnalyticsEvent(PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_STARTED)
            val serviceResult = service.setPaymentActionPaymentMethod(request)
            nextActionHandler.handle(serviceResult)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            PaymentActionResult.Failure(error)
        } finally {
            isHandlingNextAction.set(false)
        }

        // Sent after the try/catch so a wrapped unexpected Exception is reported as FAILED too.
        if (result is PaymentActionResult.Failure) {
            braintreeClient.sendAnalyticsEvent(
                PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_FAILED,
                AnalyticsEventParams(errorDescription = result.error.message),
            )
        } else {
            braintreeClient.sendAnalyticsEvent(PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_SUCCEEDED)
        }
        return result
    }

    /**
     * Drives the Payment Action forward from its current server state until a terminal outcome or
     * a handoff boundary is reached. See the suspending overload of `handleNextAction` for details.
     *
     * @param callback [PaymentActionCallback] invoked exactly once with the resulting
     *   [PaymentActionResult], or a [PaymentActionResult.Failure] describing the failure.
     */
    fun handleNextAction(callback: PaymentActionCallback) =
        coroutineScope.launch { callback.onResult(handleNextAction()) }

    /**
     * Drives the Payment Action forward from its current server state until a terminal outcome or
     * a handoff boundary is reached.
     *
     * 3D Secure device data collection is performed internally when the Payment Action requires
     * it. Any other customer action returns [PaymentActionResult.CustomerActionRequired].
     *
     * A [PaymentActionResult.Processing] result means the SDK's own bounded internal retries were
     * exhausted while the Payment Action was still settling server-side — wait briefly, then call
     * `handleNextAction` again.
     *
     * @return the resulting [PaymentActionResult], or a [PaymentActionResult.Failure] wrapping the
     *   underlying fetch/parsing error, or a [BraintreeException] if called while a previous
     *   invocation on this client hasn't completed.
     */
    suspend fun handleNextAction(): PaymentActionResult {
        if (!isHandlingNextAction.compareAndSet(false, true)) {
            return PaymentActionResult.Failure(BraintreeException(HANDLE_NEXT_ACTION_IN_PROGRESS_MESSAGE))
        }
        return try {
            nextActionHandler.handle()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            PaymentActionResult.Failure(error)
        } finally {
            isHandlingNextAction.set(false)
        }
    }

    companion object {
        internal const val HANDLE_NEXT_ACTION_IN_PROGRESS_MESSAGE =
            "handleNextAction was called while a previous invocation on this client was still in flight."
        internal const val SUBMIT_FOR_PAYMENT_ACTION_IN_PROGRESS_MESSAGE =
            "submitForPaymentAction was called while a previous invocation on this client was still in flight."
    }
}
