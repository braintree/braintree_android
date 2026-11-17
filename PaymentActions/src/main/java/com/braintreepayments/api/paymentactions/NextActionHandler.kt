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
 * At [PaymentActionStatus.REQUIRES_CUSTOMER_ACTION], 3D Secure device data collection
 * ([NextAction.ThreeDSecureAuthentication]) is resolved internally through [threeDSecureProvider]:
 * the result is signaled to the server and the loop continues on the signal's response, so a
 * frictionless flow needs no extra fetch. Device data is collected at most once per
 * [handle] call; a repeated request, or any other customer action, returns
 * [PaymentActionResult.CustomerActionRequired].
 */
internal class NextActionHandler(
    private val service: PaymentActionsService,
    private val threeDSecureProvider: ThreeDSecureProvider,
) {

    /**
     * Handles the Payment Action until it reaches a terminal status or a handoff boundary, or the
     * [PaymentActionStatus.PROCESSING] retry cap is reached.
     *
     * @return the [PaymentActionResult] for the last fetched Payment Action, or a
     * [PaymentActionResult.Failure] if a fetch, device data collection or signal fails, or the
     * Payment Action's version decreases between fetches.
     */
    @Suppress("ReturnCount")
    suspend fun handle(serviceResult: PaymentActionServiceResult? = null): PaymentActionResult {
        var lastVersion: Int? = null
        var processingAttempts = 0
        var deviceDataCollected = false
        var passedInResult: PaymentActionServiceResult? = serviceResult

        while (true) {
            val paymentAction = when (val result = passedInResult ?: service.getPaymentAction()) {
                is PaymentActionServiceResult.Success -> result.paymentAction
                is PaymentActionServiceResult.Failure -> return PaymentActionResult.Failure(result.error)
            }

            // last version increases when modified on the backend, or stays the same if unchanged, it should never decrease
            if (isVersionAnomaly(lastVersion, paymentAction.version)) {
                return PaymentActionResult.Failure(BraintreeException(VERSION_ANOMALY_MESSAGE))
            }

            lastVersion = paymentAction.version ?: lastVersion

            when (paymentAction.status) {
                PaymentActionStatus.PROCESSING -> {
                    processingAttempts++
                    if (processingAttempts >= MAX_PROCESSING_ATTEMPTS) return PaymentActionResult.Processing(paymentAction.id)
                    passedInResult = null
                    delay(PROCESSING_RETRY_DELAY)
                }

                PaymentActionStatus.REQUIRES_CUSTOMER_ACTION -> {
                    // Device data is collected at most once per call: if the server asks again, hand
                    // off rather than loop, so a misbehaving response can't keep the loop running.
                    if (paymentAction.nextAction !is NextAction.ThreeDSecureAuthentication || deviceDataCollected) {
                        return PaymentActionResult.CustomerActionRequired(paymentAction.id)
                    }
                    // The signal's response is the updated Payment Action, so the loop continues on it rather than re-fetching
                    passedInResult = when (val deviceData = threeDSecureProvider.collectDeviceData()) {
                        is DeviceDataResult.Failed -> return PaymentActionResult.Failure(deviceData.error)
                        is DeviceDataResult.Collected -> {
                            deviceDataCollected = true
                            service.signalCustomerActionComplete(
                                CustomerCompletedAction.DeviceDataCollection(deviceData.dfReferenceId)
                            )
                        }
                    }
                }

                PaymentActionStatus.REQUIRES_PAYMENT_METHOD -> return PaymentActionResult.PaymentMethodRequired(paymentAction.id)
                PaymentActionStatus.READY_FOR_CONFIRMATION ->
                    return PaymentActionResult.ServerActionRequired(paymentAction.id, ServerAction.CONFIRM)
                PaymentActionStatus.REQUIRES_CAPTURE ->
                    return PaymentActionResult.ServerActionRequired(paymentAction.id, ServerAction.CAPTURE)
                PaymentActionStatus.SUCCEEDED -> return PaymentActionResult.Completed(paymentAction.id)
                PaymentActionStatus.CANCELLED -> return PaymentActionResult.Canceled(paymentAction.id)
                PaymentActionStatus.EXPIRED -> return PaymentActionResult.Expired(paymentAction.id)
                PaymentActionStatus.UNKNOWN -> return PaymentActionResult.Unrecognized(paymentAction.id)
            }
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
