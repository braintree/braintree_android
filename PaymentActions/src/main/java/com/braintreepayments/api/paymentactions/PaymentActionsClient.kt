package com.braintreepayments.api.paymentactions

import android.content.Context
import com.braintreepayments.api.core.BraintreeClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    private val service: PaymentActionsService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher),
) {

    /**
     * Initializes a new [PaymentActionsClient] instance.
     *
     * @param context an Android [Context]
     * @param authorization a Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        PaymentActionsService(BraintreeClient(context, authorization))
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
        return service.setPaymentActionPaymentMethod(request).handleNextAction()
    }

    /**
     * Drives a payment action towards completion by fetching its current state.
     *
     * On success the [PaymentActionCallback.onResult] method will be invoked with a
     * [PaymentActionResult] variant describing the next required step.
     *
     * On failure the [PaymentActionCallback.onResult] method will be invoked with a
     * [PaymentActionResult.Failure] containing an exception.
     *
     * @param callback [PaymentActionCallback]
     */
    fun handleNextAction(callback: PaymentActionCallback) =
        coroutineScope.launch { callback.onResult(handleNextAction()) }

    /**
     * Drives a payment action towards completion by fetching its current state.
     *
     * On success a [PaymentActionResult] variant describing the next required step is returned.
     *
     * On failure [PaymentActionResult.Failure] containing an exception is returned.
     *
     * @return [PaymentActionResult]
     */
    suspend fun handleNextAction(): PaymentActionResult {
        return service.getPaymentAction().handleNextAction()
    }

    /**
     * This function is the brains of the client, which will take in the current Payment Action and
     * then perform action(s) based on what is required to push the payment towards completion.
     */
    private fun PaymentActionServiceResult.handleNextAction(): PaymentActionResult {
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
                PaymentActionStatus.CANCELED, PaymentActionStatus.EXPIRED ->
                    PaymentActionResult.Canceled(paymentAction.id)
                PaymentActionStatus.PROCESSING -> PaymentActionResult.Processing(paymentAction.id)
                PaymentActionStatus.UNKNOWN ->
                    PaymentActionResult.Failure(NotImplementedError("Received unrecognized status: ${paymentAction.status}"))
            }
            is PaymentActionServiceResult.Failure -> PaymentActionResult.Failure(error)
        }
    }
}
