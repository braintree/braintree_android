package com.braintreepayments.api.paymentactions

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.paymentactions.NextActionHandler.Companion.MAX_PROCESSING_ATTEMPTS
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Owns the Payment Action status state machine: both how the [PaymentActionsClient.handleNextAction]
 * drive loop reacts to each [PaymentActionStatus], and how each status maps to the
 * [PaymentActionResult] the merchant acts on.
 *
 * The drive loop repeatedly fetches the current Payment Action and dispatches on its status until
 * reaching a terminal outcome or a handoff boundary (a server action or a customer action is
 * required). The only status it re-polls internally is [PaymentActionStatus.PROCESSING], bounded by
 * [MAX_PROCESSING_ATTEMPTS]; on exhaustion it returns [PaymentActionResult.Processing].
 *
 * [PaymentActionStatus.REQUIRES_CUSTOMER_ACTION] is returned immediately without further looping.
 * This is what enforces the customer-action boundary: this handler never inspects
 * [PaymentAction.nextAction] or drives 3D Secure itself.
 */
internal class NextActionHandler(
    private val service: PaymentActionsService,
) {

    /**
     * Handles the Payment Action until it reaches a terminal status or a handoff boundary, or the
     * [PaymentActionStatus.PROCESSING] retry cap is reached.
     *
     * @return the [PaymentActionResult] for the last fetched Payment Action, or a
     *   [PaymentActionResult.Failure] if a fetch fails or the Payment Action's version decreases
     *   between fetches.
     */
    @Suppress("ReturnCount")
    suspend fun handle(): PaymentActionResult {
        var lastVersion: Int? = null
        var processingAttempts = 0
        while (true) {
            val paymentAction = when (val result = service.getPaymentAction()) {
                is PaymentActionServiceResult.Success -> result.paymentAction
                is PaymentActionServiceResult.Failure -> return PaymentActionResult.Failure(result.error)
            }

            if (isVersionAnomaly(lastVersion, paymentAction.version)) {
                return PaymentActionResult.Failure(BraintreeException(VERSION_ANOMALY_MESSAGE))
            }
            lastVersion = paymentAction.version ?: lastVersion

            val id = paymentAction.id
            when (paymentAction.status) {
                PaymentActionStatus.PROCESSING -> {
                    processingAttempts++
                    if (processingAttempts >= MAX_PROCESSING_ATTEMPTS) return PaymentActionResult.Processing(id)
                    delay(PROCESSING_RETRY_DELAY)
                }

                PaymentActionStatus.REQUIRES_CUSTOMER_ACTION -> return PaymentActionResult.CustomerActionRequired(id)

                PaymentActionStatus.REQUIRES_PAYMENT_METHOD -> return PaymentActionResult.PaymentMethodRequired(id)
                PaymentActionStatus.READY_FOR_CONFIRMATION ->
                    return PaymentActionResult.ServerActionRequired(id, ServerAction.CONFIRM)
                PaymentActionStatus.REQUIRES_CAPTURE ->
                    return PaymentActionResult.ServerActionRequired(id, ServerAction.CAPTURE)
                PaymentActionStatus.SUCCEEDED -> return PaymentActionResult.Completed(id)
                PaymentActionStatus.CANCELLED -> return PaymentActionResult.Canceled(id)
                PaymentActionStatus.EXPIRED -> return PaymentActionResult.Expired(id)
                PaymentActionStatus.UNKNOWN -> return PaymentActionResult.Unrecognized(id)
            }
        }
    }

    /**
     * Maps a single service response to the [PaymentActionResult] the merchant acts on, without
     * handling it further. Used by [PaymentActionsClient.submitForPaymentAction], which reports the
     * submitted Payment Action as-is rather than polling it.
     */
    fun toPaymentActionResult(serviceResult: PaymentActionServiceResult): PaymentActionResult =
        when (serviceResult) {
            is PaymentActionServiceResult.Success -> toPaymentActionResult(serviceResult.paymentAction)
            is PaymentActionServiceResult.Failure -> PaymentActionResult.Failure(serviceResult.error)
        }

    private fun toPaymentActionResult(paymentAction: PaymentAction): PaymentActionResult {
        val id = paymentAction.id
        return when (paymentAction.status) {
            PaymentActionStatus.REQUIRES_PAYMENT_METHOD -> PaymentActionResult.PaymentMethodRequired(id)
            PaymentActionStatus.REQUIRES_CUSTOMER_ACTION -> PaymentActionResult.CustomerActionRequired(id)
            PaymentActionStatus.READY_FOR_CONFIRMATION ->
                PaymentActionResult.ServerActionRequired(id, ServerAction.CONFIRM)
            PaymentActionStatus.REQUIRES_CAPTURE -> PaymentActionResult.ServerActionRequired(id, ServerAction.CAPTURE)
            PaymentActionStatus.SUCCEEDED -> PaymentActionResult.Completed(id)
            PaymentActionStatus.CANCELLED -> PaymentActionResult.Canceled(id)
            PaymentActionStatus.EXPIRED -> PaymentActionResult.Expired(id)
            PaymentActionStatus.PROCESSING -> PaymentActionResult.Processing(id)
            PaymentActionStatus.UNKNOWN -> PaymentActionResult.Unrecognized(id)
        }
    }

    /**
     * A version should never decrease within a single drive loop; if it does, the response is stale
     * or out of order. Equal or missing versions are expected (e.g. repeated `PROCESSING` fetches).
     */
    private fun isVersionAnomaly(lastVersion: Int?, version: Int?): Boolean =
        lastVersion != null && version != null && version < lastVersion

    companion object {
        internal const val MAX_PROCESSING_ATTEMPTS = 3
        internal val PROCESSING_RETRY_DELAY: Duration = 1.seconds
        internal const val VERSION_ANOMALY_MESSAGE =
            "Payment Action version decreased between fetches during handleNextAction."
    }
}
