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
 * At [PaymentActionStatus.REQUIRES_CUSTOMER_ACTION], the 3D Secure steps are resolved internally
 * through [threeDSecureProvider]: device data collection ([NextAction.ThreeDSecureAuthentication])
 * and the challenge ([NextAction.ThreeDSecureChallenge]). Each result is signaled to the server and the
 * loop continues on the signal's response, so no extra fetch is needed. Each step runs at most once per
 * [handle] call. An abandoned challenge returns [PaymentActionResult.ThreeDSecureCanceled]; a repeated
 * request, or any other customer action, returns [PaymentActionResult.CustomerActionRequired].
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
     * [PaymentActionResult.Failure] if a fetch, device data collection, the challenge or a signal
     * fails, or the Payment Action's version decreases between fetches.
     */
    @Suppress("ReturnCount")
    suspend fun handle(serviceResult: PaymentActionServiceResult? = null): PaymentActionResult {
        var lastVersion: Int? = null
        var processingAttempts = 0
        var deviceDataCollected = false
        var challengePresented = false
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
                    val nextAction = paymentAction.nextAction
                    val outcome = when {
                        nextAction is NextAction.ThreeDSecureAuthentication && !deviceDataCollected -> {
                            deviceDataCollected = true
                            collectDeviceData()
                        }
                        nextAction is NextAction.ThreeDSecureChallenge && !challengePresented -> {
                            challengePresented = true
                            presentChallenge(paymentAction.id, nextAction)
                        }
                        else -> return PaymentActionResult.CustomerActionRequired(paymentAction.id)
                    }
                    // Continue on the signal's response instead of re-fetching.
                    passedInResult = when (outcome) {
                        is CustomerActionOutcome.Advanced -> outcome.response
                        is CustomerActionOutcome.Stop -> return outcome.result
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

    private suspend fun collectDeviceData(): CustomerActionOutcome =
        when (val deviceData = threeDSecureProvider.collectDeviceData()) {
            is DeviceDataResult.Failed -> CustomerActionOutcome.Stop(PaymentActionResult.Failure(deviceData.error))
            is DeviceDataResult.Collected -> CustomerActionOutcome.Advanced(
                service.signalCustomerActionComplete(
                    CustomerCompletedAction.DeviceDataCollection(deviceData.dfReferenceId)
                )
            )
        }

    private suspend fun presentChallenge(
        paymentActionId: String,
        challenge: NextAction.ThreeDSecureChallenge,
    ): CustomerActionOutcome {
        // Checked before launching, so the shell never flashes for a challenge that can't be shown.
        val transactionId = challenge.mpiTransactionId ?: return CustomerActionOutcome.Stop(
            PaymentActionResult.Failure(BraintreeException(MISSING_MPI_TRANSACTION_ID_MESSAGE))
        )

        return when (val result = threeDSecureProvider.presentChallenge(transactionId, challenge.challengePayload)) {
            is ChallengeResult.Completed -> CustomerActionOutcome.Advanced(
                service.signalCustomerActionComplete(CustomerCompletedAction.Challenge(result.serverJwt))
            )
            ChallengeResult.Abandoned ->
                CustomerActionOutcome.Stop(PaymentActionResult.ThreeDSecureCanceled(paymentActionId))
            is ChallengeResult.Failed -> CustomerActionOutcome.Stop(PaymentActionResult.Failure(result.error))
        }
    }

    /** What the drive loop does after resolving a customer action. */
    private sealed class CustomerActionOutcome {

        /** The server was signaled; the loop continues on its [response]. */
        class Advanced(val response: PaymentActionServiceResult) : CustomerActionOutcome()

        /** The loop stops and returns [result]. */
        class Stop(val result: PaymentActionResult) : CustomerActionOutcome()
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
        internal const val MISSING_MPI_TRANSACTION_ID_MESSAGE =
            "The 3D Secure challenge is missing its required mpiTransactionId."
    }
}
