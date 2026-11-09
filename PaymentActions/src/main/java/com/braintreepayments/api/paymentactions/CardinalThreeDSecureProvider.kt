package com.braintreepayments.api.paymentactions

import android.content.Context
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

/**
 * [ThreeDSecureProvider] backed by the Cardinal Mobile SDK.
 *
 * Cardinal on Android is a process-wide singleton. Every [collectDeviceData] call configures and
 * initializes a fresh Cardinal session.
 */
internal class CardinalThreeDSecureProvider(
    private val appContext: Context,
    private val braintreeClient: BraintreeClient,
) : ThreeDSecureProvider {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun collectDeviceData(): DeviceDataResult {
        // Read lazily so merchants who never reach 3D Secure never fail on missing 3DS configuration.
        val configuration = try {
            braintreeClient.getConfiguration()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            return DeviceDataResult.Failed(error)
        }

        val jwt = configuration.cardinalAuthenticationJwt
        if (jwt.isNullOrEmpty()) {
            return DeviceDataResult.Failed(BraintreeException(MISSING_CARDINAL_JWT_MESSAGE))
        }

        return try {
            Cardinal.getInstance().configure(appContext, cardinalParameters(configuration.environment))
            setUpSession(jwt)
        } catch (error: RuntimeException) {
            if (error is CancellationException) throw error
            DeviceDataResult.Failed(BraintreeException(CARDINAL_SETUP_ERROR_MESSAGE, error))
        }
    }

    /**
     * Runs Cardinal's session setup and suspends until Cardinal calls back.
     */
    private suspend fun setUpSession(jwt: String): DeviceDataResult =
        suspendCancellableCoroutine { continuation ->
            // Cardinal calls back on its own thread and may call more than once; resuming a
            // continuation twice crashes, so only the first callback wins.
            val resumed = AtomicBoolean(false)
            fun resumeOnce(result: DeviceDataResult) {
                if (resumed.compareAndSet(false, true)) continuation.resume(result)
            }

            val initService = object : CardinalInitService {
                // Cardinal's session id is the value the server calls dfReferenceId.
                override fun onSetupCompleted(sessionId: String) {
                    resumeOnce(DeviceDataResult.Collected(sessionId))
                }

                // During setup, Cardinal calls onValidated only when setup fails, e.g. an invalid JWT.
                override fun onValidated(validateResponse: ValidateResponse?, serverJwt: String?) {
                    resumeOnce(
                        DeviceDataResult.Failed(BraintreeException(CARDINAL_SETUP_VALIDATION_FAILED_MESSAGE))
                    )
                }
            }

            Cardinal.getInstance().init(jwt, initService)
        }

    private fun cardinalParameters(braintreeEnvironment: String?): CardinalConfigurationParameters {
        val isProduction = PRODUCTION_ENVIRONMENT.equals(braintreeEnvironment, ignoreCase = true)
        return CardinalConfigurationParameters().apply {
            environment = if (isProduction) CardinalEnvironment.PRODUCTION else CardinalEnvironment.STAGING
            requestTimeout = REQUEST_TIMEOUT_MS
            challengeTimeout = CHALLENGE_TIMEOUT_MINUTES
            // Holds back onSetupCompleted until device data has reached Cardinal, so the server never
            // receives a dfReferenceId whose device data is still in flight.
            isEnableDFSync = true
            isEnableLogging = !isProduction
            // Legacy default
            uiType = CardinalUiType.BOTH
        }
    }

    companion object {
        private const val PRODUCTION_ENVIRONMENT = "production"
        internal const val REQUEST_TIMEOUT_MS = 8_000

        /** Cardinal's documented minimum (and default) challenge timeout, in minutes. */
        internal const val CHALLENGE_TIMEOUT_MINUTES = 5

        internal const val MISSING_CARDINAL_JWT_MESSAGE =
            "Payment Actions configuration is missing the Cardinal authentication JWT."
        internal const val CARDINAL_SETUP_VALIDATION_FAILED_MESSAGE =
            "Cardinal failed to validate the Cardinal authentication JWT during session setup."
        internal const val CARDINAL_SETUP_ERROR_MESSAGE = "Cardinal SDK setup error."
    }
}
