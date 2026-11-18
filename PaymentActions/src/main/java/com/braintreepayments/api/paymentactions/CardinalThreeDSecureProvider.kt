package com.braintreepayments.api.paymentactions

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * [ThreeDSecureProvider] backed by the Cardinal Mobile SDK, and the only class that touches Cardinal.
 * It's also the [ChallengeRegistrar] the challenge shell uses to reach Cardinal.
 *
 * Cardinal on Android is a process-wide singleton. [collectDeviceData] starts a new Cardinal session and
 * [presentChallenge] continues on it, as the legacy flow does. [presentChallenge] sets Cardinal up again
 * only when this provider has no session in this process, e.g. after process death.
 *
 * @param createObserver replaced in tests; the real constructor needs a set-up Cardinal SDK.
 */
internal class CardinalThreeDSecureProvider(
    private val appContext: Context,
    private val braintreeClient: BraintreeClient,
    private val challengePresenter: ChallengePresenter =
        ChallengePresenter(appContext, watchdogTimeout = CHALLENGE_WATCHDOG_TIMEOUT),
    private val createObserver: (FragmentActivity, CardinalValidateReceiver) -> CardinalChallengeObserver =
        ::CardinalChallengeObserver,
) : ThreeDSecureProvider, ChallengeRegistrar {

    /** Whether the live Cardinal session in this process is one this provider set up. */
    @Volatile
    private var hasSession = false

    override suspend fun collectDeviceData(): DeviceDataResult = setUp()

    override suspend fun presentChallenge(transactionId: String, payload: String): ChallengeResult {
        // Continue on the device data session. Without one (e.g. after process death), cca_continue throws, so set Cardinal up first.
        if (!hasSession) {
            val setup = setUp()
            if (setup is DeviceDataResult.Failed) return ChallengeResult.Failed(setup.error)
        }
        val result = challengePresenter.present(transactionId, payload, this)
        // The session may be gone (e.g. another module called Cardinal.cleanup()), so set up again next time.
        if (result is ChallengeResult.Failed) hasSession = false
        return result
    }

    override fun register(activity: FragmentActivity, onResult: (ChallengeResult) -> Unit): ChallengeStarter {
        // Must be built during onCreate: it registers an Activity result launcher.
        val observer = createObserver(
            activity,
            CardinalValidateReceiver { _, validateResponse, serverJwt ->
                onResult(
                    toChallengeResult(validateResponse?.actionCode, serverJwt, validateResponse?.errorDescription)
                )
            },
        )
        return ChallengeStarter { transactionId, payload ->
            // Never call Cardinal.cleanup() before this: it wipes the session and cca_continue throws.
            Cardinal.getInstance().cca_continue(transactionId, payload, observer)
        }
    }

    /**
     * Configures and initializes Cardinal. Initializing performs device data collection; Cardinal's
     * session id is the value the server calls dfReferenceId.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun setUp(): DeviceDataResult {
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
            // A new setup replaces any earlier session; there's no session until this one completes.
            hasSession = false
            Cardinal.getInstance().configure(appContext, cardinalParameters(configuration.environment))
            setUpSession(jwt).also { hasSession = it is DeviceDataResult.Collected }
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
                    val message = validateResponse?.errorDescription
                        ?.takeIf { it.isNotBlank() }
                        ?: CARDINAL_SETUP_VALIDATION_FAILED_MESSAGE
                    resumeOnce(DeviceDataResult.Failed(BraintreeException(message)))
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

        /** Cardinal's challenge timeout plus one minute, to catch Cardinal going silent. */
        internal val CHALLENGE_WATCHDOG_TIMEOUT: Duration = (CHALLENGE_TIMEOUT_MINUTES + 1).minutes

        internal const val MISSING_CARDINAL_JWT_MESSAGE =
            "Payment Actions configuration is missing the Cardinal authentication JWT."
        internal const val CARDINAL_SETUP_VALIDATION_FAILED_MESSAGE =
            "Cardinal failed to validate the Cardinal authentication JWT during session setup."
        internal const val CARDINAL_SETUP_ERROR_MESSAGE = "Cardinal SDK setup error."
        internal const val MISSING_SERVER_JWT_MESSAGE =
            "Cardinal reported a completed 3D Secure challenge without a server JWT."
        internal const val CHALLENGE_ERROR_MESSAGE = "Cardinal reported an error during the 3D Secure challenge."
        internal const val UNRECOGNIZED_ACTION_CODE_MESSAGE =
            "Cardinal returned a 3D Secure action code this SDK version does not recognize:"

        /** Maps every action code, including unknown ones, so the loop never waits on an unmapped code. */
        internal fun toChallengeResult(
            actionCode: CardinalActionCode?,
            serverJwt: String?,
            errorDescription: String?,
        ): ChallengeResult = when (actionCode) {
            // FAILURE is a failed authentication, not an error
            CardinalActionCode.SUCCESS, CardinalActionCode.NOACTION, CardinalActionCode.FAILURE ->
                if (serverJwt.isNullOrEmpty()) {
                    ChallengeResult.Failed(BraintreeException(MISSING_SERVER_JWT_MESSAGE))
                } else {
                    ChallengeResult.Completed(serverJwt)
                }

            // A back press reports CANCEL or TIMEOUT depending on the device.
            CardinalActionCode.CANCEL, CardinalActionCode.TIMEOUT -> ChallengeResult.Abandoned

            CardinalActionCode.ERROR -> ChallengeResult.Failed(
                BraintreeException(errorDescription?.takeIf { it.isNotBlank() } ?: CHALLENGE_ERROR_MESSAGE)
            )

            else -> ChallengeResult.Failed(BraintreeException("$UNRECOGNIZED_ACTION_CODE_MESSAGE $actionCode"))
        }
    }
}
