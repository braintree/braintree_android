package com.braintreepayments.api.paymentactions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.InvalidArgumentException
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalEnvironment
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalRenderType
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalChallengeObserver
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalValidateReceiver
import com.cardinalcommerce.shared.models.enums.ButtonType
import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization
import com.cardinalcommerce.shared.userinterfaces.LabelCustomization
import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization
import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization
import com.cardinalcommerce.shared.userinterfaces.UiCustomization
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * [ThreeDSecureProvider] backed by the Cardinal Mobile SDK. This file is the only place that touches
 * Cardinal. The provider is also the [ChallengeRegistrar] the challenge shell uses to reach Cardinal.
 *
 * Cardinal on Android is a process-wide singleton. [collectDeviceData] starts a new Cardinal session and
 * [presentChallenge] continues on it, as the legacy flow does. [presentChallenge] sets Cardinal up again
 * only when this provider has no session in this process, e.g. after process death.
 *
 * @param launchOptions the merchant's options for presenting the challenge, applied at every setup.
 * @param createObserver replaced in tests; the real constructor needs a set-up Cardinal SDK.
 */
internal class CardinalThreeDSecureProvider(
    private val appContext: Context,
    private val braintreeClient: BraintreeClient,
    private val launchOptions: PaymentActionThreeDSecureOptions = PaymentActionThreeDSecureOptions(),
    private val challengeCoordinator: ChallengeCoordinator =
        ChallengeCoordinator(appContext, watchdogTimeout = CHALLENGE_WATCHDOG_TIMEOUT),
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
        val result = challengeCoordinator.startChallenge(transactionId, payload, this)
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
        // Checked first: if no activity here handles the URL, a bank app can't bring the customer back.
        val requestorAppUrl = launchOptions.requestorAppUrl
        if (requestorAppUrl != null && !isHandledByThisApp(requestorAppUrl)) {
            return DeviceDataResult.Failed(
                InvalidArgumentException(requestorAppUrlNotHandledMessage(requestorAppUrl))
            )
        }

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

    /**
     * Whether an activity in this app opens [url] from an implicit `VIEW` intent, as a bank app would open it.
     */
    @SuppressLint("QueryPermissionsNeeded")
    private fun isHandledByThisApp(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).setPackage(appContext.packageName)
        return intent.resolveActivity(appContext.packageManager) != null
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

            // Options left null keep Cardinal's defaults
            uiType = launchOptions.uiType.toCardinal()
            launchOptions.renderTypes?.let { renderTypes ->
                renderType = JSONArray().apply { renderTypes.forEach { put(it.toCardinal()) } }
            }
            launchOptions.uiCustomization?.let { uiCustomization = it.toCardinal() }
            launchOptions.requestorAppUrl?.let { threeDSRequestorAppURL = it }
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

        internal fun requestorAppUrlNotHandledMessage(url: String) =
            "No activity in this app handles the 3D Secure requestorAppUrl \"$url\". Declare an intent filter " +
                "for it with the VIEW action, the DEFAULT category and a matching <data> element."

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

// Maps the public launch options onto Cardinal's types

private fun PaymentActionThreeDSecureUiType.toCardinal(): CardinalUiType = when (this) {
    PaymentActionThreeDSecureUiType.NATIVE -> CardinalUiType.NATIVE
    PaymentActionThreeDSecureUiType.HTML -> CardinalUiType.HTML
    PaymentActionThreeDSecureUiType.BOTH -> CardinalUiType.BOTH
}

private fun PaymentActionThreeDSecureRenderType.toCardinal(): CardinalRenderType = when (this) {
    PaymentActionThreeDSecureRenderType.OTP -> CardinalRenderType.OTP
    PaymentActionThreeDSecureRenderType.SINGLE_SELECT -> CardinalRenderType.SINGLE_SELECT
    PaymentActionThreeDSecureRenderType.MULTI_SELECT -> CardinalRenderType.MULTI_SELECT
    PaymentActionThreeDSecureRenderType.OOB -> CardinalRenderType.OOB
    PaymentActionThreeDSecureRenderType.RENDER_HTML -> CardinalRenderType.HTML
}

private fun PaymentActionThreeDSecureButtonType.toCardinal(): ButtonType = when (this) {
    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_VERIFY -> ButtonType.VERIFY
    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_CONTINUE -> ButtonType.CONTINUE
    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_NEXT -> ButtonType.NEXT
    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_CANCEL -> ButtonType.CANCEL
    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_RESEND -> ButtonType.RESEND
}

private fun PaymentActionThreeDSecureUiCustomization.toCardinal(): UiCustomization =
    UiCustomization().also { cardinal ->
        buttonCustomizations.forEach { (type, button) ->
            cardinal.setButtonCustomization(button.toCardinal(), type.toCardinal())
        }
        labelCustomization?.let { cardinal.labelCustomization = it.toCardinal() }
        textBoxCustomization?.let { cardinal.textBoxCustomization = it.toCardinal() }
        toolbarCustomization?.let { cardinal.toolbarCustomization = it.toCardinal() }
    }

// In the customizations below, a null text or a 0 number means "not set", so Cardinal keeps its default.

private fun PaymentActionThreeDSecureButtonCustomization.toCardinal(): ButtonCustomization =
    ButtonCustomization().also { cardinal ->
        textFontName?.let { cardinal.textFontName = it }
        textColor?.let { cardinal.textColor = it }
        if (textFontSize != 0) cardinal.textFontSize = textFontSize
        backgroundColor?.let { cardinal.backgroundColor = it }
        if (cornerRadius != 0) cardinal.cornerRadius = cornerRadius
    }

private fun PaymentActionThreeDSecureLabelCustomization.toCardinal(): LabelCustomization =
    LabelCustomization().also { cardinal ->
        textFontName?.let { cardinal.textFontName = it }
        textColor?.let { cardinal.textColor = it }
        if (textFontSize != 0) cardinal.textFontSize = textFontSize
        headingTextColor?.let { cardinal.headingTextColor = it }
        headingTextFontName?.let { cardinal.headingTextFontName = it }
        if (headingTextFontSize != 0) cardinal.headingTextFontSize = headingTextFontSize
    }

private fun PaymentActionThreeDSecureTextBoxCustomization.toCardinal(): TextBoxCustomization =
    TextBoxCustomization().also { cardinal ->
        textFontName?.let { cardinal.textFontName = it }
        textColor?.let { cardinal.textColor = it }
        if (textFontSize != 0) cardinal.textFontSize = textFontSize
        if (borderWidth != 0) cardinal.borderWidth = borderWidth
        borderColor?.let { cardinal.borderColor = it }
        if (cornerRadius != 0) cardinal.cornerRadius = cornerRadius
    }

private fun PaymentActionThreeDSecureToolbarCustomization.toCardinal(): ToolbarCustomization =
    ToolbarCustomization().also { cardinal ->
        textFontName?.let { cardinal.textFontName = it }
        textColor?.let { cardinal.textColor = it }
        if (textFontSize != 0) cardinal.textFontSize = textFontSize
        backgroundColor?.let { cardinal.backgroundColor = it }
        headerText?.let { cardinal.headerText = it }
        buttonText?.let { cardinal.buttonText = it }
    }
