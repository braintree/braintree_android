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
    private val service: PaymentActionsService = PaymentActionsService(braintreeClient),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher),
) {

    private val nextActionHandler = NextActionHandler(service)

    /**
     * Tracks whether a [handleNextAction] drive loop is already in flight on this client instance.
     */
    private val isHandlingNextAction = AtomicBoolean(false)

    /**
     * Initializes a new [PaymentActionsClient] instance.
     *
     * @param context an Android [Context]
     * @param authorization a Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        BraintreeClient(context, authorization)
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
     * @param request [PaymentActionRequest]
     * @return [PaymentActionResult]
     */
    suspend fun submitForPaymentAction(request: PaymentActionRequest): PaymentActionResult {
        braintreeClient.sendAnalyticsEvent(PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_STARTED)
        return service.setPaymentActionPaymentMethod(request).toPaymentActionResult().also { result ->
            if (result is PaymentActionResult.Failure) {
                braintreeClient.sendAnalyticsEvent(
                    PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_FAILED,
                    AnalyticsEventParams(errorDescription = result.error.message),
                )
            } else {
                braintreeClient.sendAnalyticsEvent(PaymentActionsAnalytics.SET_PAYMENT_ACTION_PAYMENT_METHOD_SUCCEEDED)
            }
        }
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
     * This never starts device data collection or a 3D Secure challenge itself — if the Payment
     * Action requires a customer action, this returns [PaymentActionResult.CustomerActionRequired]
     * rather than driving the customer action, since that boundary is owned by a separate flow.
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
            nextActionHandler.drive().toPaymentActionResult()
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            PaymentActionResult.Failure(error)
        } finally {
            isHandlingNextAction.set(false)
        }
    }

    /**
     * Maps the raw [PaymentActionServiceResult] from the service into the [PaymentActionResult]
     * the merchant acts on.
     */
    private fun PaymentActionServiceResult.toPaymentActionResult(): PaymentActionResult {
        return when (this) {
            is PaymentActionServiceResult.Success -> when (paymentAction.status) {
                PaymentActionStatus.REQUIRES_PAYMENT_METHOD ->
                    PaymentActionResult.PaymentMethodRequired(paymentAction.id)
                PaymentActionStatus.REQUIRES_CUSTOMER_ACTION -> PaymentActionResult.CustomerActionRequired(
                    paymentAction.id,
                )
                PaymentActionStatus.READY_FOR_CONFIRMATION ->
                    PaymentActionResult.ServerActionRequired(paymentAction.id, ServerAction.CONFIRM)
                PaymentActionStatus.REQUIRES_CAPTURE ->
                    PaymentActionResult.ServerActionRequired(paymentAction.id, ServerAction.CAPTURE)
                PaymentActionStatus.SUCCEEDED -> PaymentActionResult.Completed(paymentAction.id)
                PaymentActionStatus.CANCELLED -> PaymentActionResult.Canceled(paymentAction.id)
                PaymentActionStatus.EXPIRED -> PaymentActionResult.Expired(paymentAction.id)
                PaymentActionStatus.PROCESSING -> PaymentActionResult.Processing(paymentAction.id)
                PaymentActionStatus.UNKNOWN ->
                    PaymentActionResult.Unknown(paymentAction.id)
            }
            is PaymentActionServiceResult.Failure -> PaymentActionResult.Failure(error)
        }
    }

    companion object {
        internal const val HANDLE_NEXT_ACTION_IN_PROGRESS_MESSAGE =
            "handleNextAction was called while a previous invocation on this client was still in flight."
    }
}
