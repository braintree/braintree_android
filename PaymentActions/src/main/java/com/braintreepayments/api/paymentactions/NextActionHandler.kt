package com.braintreepayments.api.paymentactions

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.paymentactions.NextActionHandler.Companion.MAX_PROCESSING_ATTEMPTS
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Owns the Payment Action status state machine for the [PaymentActionsClient.handleNextAction]
 * drive loop.
 *
 * Repeatedly fetches the current Payment Action and dispatches on its status until reaching a
 * terminal outcome or a handoff boundary (a server action or a customer action is required). The
 * only status this loop re-polls internally is [PaymentActionStatus.PROCESSING], bounded by
 * [MAX_PROCESSING_ATTEMPTS]; on exhaustion the last `PROCESSING` Payment Action is returned.
 *
 * [PaymentActionStatus.REQUIRES_CUSTOMER_ACTION] is returned immediately without further looping.
 * This is what enforces the customer-action boundary: this handler never inspects
 * [PaymentAction.nextAction] or drives 3D Secure itself.
 */
internal class NextActionHandler(
    private val service: PaymentActionsService,
) {

    /**
     * Drives the Payment Action until it leaves [PaymentActionStatus.PROCESSING] or the retry cap
     * is reached.
     *
     * @return the last fetched [PaymentActionServiceResult], or a [PaymentActionServiceResult.Failure]
     *   if a fetch fails or the Payment Action's version decreases between fetches.
     */
    suspend fun drive(): PaymentActionServiceResult {
        var lastVersion: Int? = null
        var processingAttempts = 0
        while (true) {
            val result = service.getPaymentAction()
            if (result !is PaymentActionServiceResult.Success) return result

            val paymentAction = result.paymentAction
            if (isVersionAnomaly(lastVersion, paymentAction.version)) {
                return PaymentActionServiceResult.Failure(BraintreeException(VERSION_ANOMALY_MESSAGE))
            }
            lastVersion = paymentAction.version ?: lastVersion

            processingAttempts++
            if (paymentAction.status != PaymentActionStatus.PROCESSING ||
                processingAttempts >= MAX_PROCESSING_ATTEMPTS
            ) {
                return result
            }
            delay(PROCESSING_RETRY_DELAY)
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
