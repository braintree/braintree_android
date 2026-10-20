package com.braintreepayments.api.paypal

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultRegistry
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.AppSwitchRepository
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.usecase.GetAppSwitchUseCase
import com.braintreepayments.api.core.MerchantRepository

/**
 * Responsible for launching PayPal user authentication in a web browser
 */
class PayPalLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient,
    private val getAppSwitchUseCase: GetAppSwitchUseCase = GetAppSwitchUseCase(AppSwitchRepository.instance),
    private val resolvePayPalUseCase: ResolvePayPalUseCase =
        ResolvePayPalUseCase(MerchantRepository.instance),
    lazyAnalyticsClient: Lazy<AnalyticsClient>,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance
) {
    /**
     * Used to launch the PayPal flow in a web browser and deliver results to your Activity
     * @param caller Optional ActivityResultCaller parameter. If provided, it will be passed to BrowserSwitchClient
     */
    @JvmOverloads
    constructor(caller: ActivityResultCaller? = null) : this(
        browserSwitchClient = if (caller != null) BrowserSwitchClient(caller) else BrowserSwitchClient(),
        lazyAnalyticsClient = AnalyticsClient.lazyInstance
    )

    /**
     * Used to launch the PayPal flow in a web browser and deliver results. Use it in Compose flows or where
     * ActivityResultCaller is not available.
     * @param registry ActivityResultRegistry parameter. It will be passed to BrowserSwitchClient.
     */
    constructor(registry: ActivityResultRegistry) : this(
        browserSwitchClient = BrowserSwitchClient(registry),
        lazyAnalyticsClient = AnalyticsClient.lazyInstance
    )

    @Throws(BrowserSwitchException::class)
    fun restorePendingRequest(pendingRequestString: String) {
        browserSwitchClient.restorePendingRequest(pendingRequestString)
    }

    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient

    private var startedRequest: Pair<String, Authorization>? = null

    /**
     * Launches the PayPal flow by switching to a web browser for user authentication
     *
     * @param activity the Android Activity from which you will launch the web browser
     * @param paymentAuthRequest a [PayPalPaymentAuthRequest.ReadyToLaunch] received from
     * calling [PayPalClient.createPaymentAuthRequest]
     * @return [PayPalPendingRequest] a [PayPalPendingRequest.Started] should be stored
     * to complete the flow upon return to app in
     * [PayPalLauncher.handleReturnToApp],
     * or a [PayPalPendingRequest.Failure] with an error if the PayPal flow was unable to be
     * launched in a browser.
     */
    fun launch(
        activity: ComponentActivity,
        paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch
    ): PayPalPendingRequest {
        val contextId = paymentAuthRequest.requestParams.contextId
        val analyticsEventParams = AnalyticsEventParams(
            contextId = contextId,
            appSwitchUrl = paymentAuthRequest.requestParams.approvalUrl,
            isVaultRequest = paymentAuthRequest.requestParams.isVaultRequest,
        )
        val authorization = paymentAuthRequest.requestParams.authorization

        setLaunchAnalyticAttributes(paymentAuthRequest.requestParams)
        val isAppSwitch = processAppSwitchAttempt(analyticsEventParams, authorization)

        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.requestParams)
        } catch (browserSwitchException: BrowserSwitchException) {
            val manifestInvalidError = createBrowserSwitchError(browserSwitchException)
            return sendLaunchFailureEventAndReturn(
                manifestInvalidError,
                isAppSwitch,
                analyticsEventParams,
                authorization
            )
        }

        val options = paymentAuthRequest.requestParams.browserSwitchOptions
            ?: return buildLaunchFailureAndReturn(
                errorMessage = "BrowserSwitchOptions is null",
                isAppSwitch = isAppSwitch,
                analyticsEventParams = analyticsEventParams,
                authorization = authorization
            )

        options.url
            ?: return buildLaunchFailureAndReturn(
                errorMessage = "BrowserSwitchOptions URL is null",
                isAppSwitch = isAppSwitch,
                analyticsEventParams = analyticsEventParams,
                authorization = authorization
            )

        return handleStartResult(
            browserSwitchClient.start(activity, options, isAppSwitch),
            isAppSwitch,
            analyticsEventParams,
            authorization
        )
    }

    private fun handleStartResult(
        request: BrowserSwitchStartResult,
        isAppSwitch: Boolean,
        analyticsEventParams: AnalyticsEventParams,
        authorization: Authorization?
    ): PayPalPendingRequest {
        return when (request) {
            is BrowserSwitchStartResult.Failure -> {
                val errorEvent = if (isAppSwitch) {
                    PayPalAnalytics.APP_SWITCH_FAILED
                } else {
                    PayPalAnalytics.BROWSER_PRESENTATION_FAILED
                }
                sendEvent(
                    eventName = errorEvent,
                    params = analyticsEventParams.copy(errorDescription = request.error.message),
                    authorization = authorization
                )
                PayPalPendingRequest.Failure(request.error)
            }

            is BrowserSwitchStartResult.Started -> {
                val event = if (isAppSwitch) {
                    PayPalAnalytics.APP_SWITCH_SUCCEEDED
                } else {
                    PayPalAnalytics.BROWSER_PRESENTATION_SUCCEEDED
                }
                authorization?.let { startedRequest = request.pendingRequest to it }
                sendEvent(
                    eventName = event,
                    params = analyticsEventParams,
                    authorization = authorization
                )
                PayPalPendingRequest.Started(request.pendingRequest)
            }
        }
    }

    /**
     * Captures and delivers the result of a PayPal authentication flow.
     *
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke
     * [PayPalLauncher.launch].
     *
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     *
     * @param pendingRequest the [PayPalPendingRequest.Started] stored after successfully
     * invoking [PayPalLauncher.launch]
     * @param intent         the intent to return to your application containing a deep link result
     * from the PayPal browser flow
     * @return a [PayPalPaymentAuthResult.Success] that should be passed to [PayPalClient.tokenize]
     * to complete the PayPal payment flow. Returns [PayPalPaymentAuthResult.NoResult] if the user
     * canceled the payment flow, or returned to the app without completing the PayPal
     * authentication flow.
     */
    fun handleReturnToApp(
        pendingRequest: PayPalPendingRequest.Started,
        intent: Intent
    ): PayPalPaymentAuthResult {
        val contextId = intent.data?.getQueryParameter("ba_token") ?: intent.data?.getQueryParameter("token")
        val analyticsEventParams = AnalyticsEventParams(
            contextId = contextId,
            appSwitchUrl = intent.data?.toString()
        )
        val authorization = startedRequest
            ?.takeIf { it.first == pendingRequest.pendingRequestString }
            ?.second

        sendEvent(PayPalAnalytics.HANDLE_RETURN_STARTED, analyticsEventParams, authorization)

        return when (
            val browserSwitchResult = browserSwitchClient.completeRequest(intent, pendingRequest.pendingRequestString)
        ) {
            is BrowserSwitchFinalResult.Success -> {
                sendEvent(PayPalAnalytics.HANDLE_RETURN_SUCCEEDED, analyticsEventParams, authorization)
                if (authorization != null) startedRequest = null
                PayPalPaymentAuthResult.Success(browserSwitchResult)
            }

            is BrowserSwitchFinalResult.Failure -> {
                sendEvent(
                    PayPalAnalytics.HANDLE_RETURN_FAILED,
                    analyticsEventParams.copy(
                        errorDescription = browserSwitchResult.error.message
                    ),
                    authorization
                )
                if (authorization != null) startedRequest = null
                PayPalPaymentAuthResult.Failure(browserSwitchResult.error)
            }

            is BrowserSwitchFinalResult.NoResult -> {
                sendEvent(PayPalAnalytics.HANDLE_RETURN_NO_RESULT, analyticsEventParams, authorization)
                PayPalPaymentAuthResult.NoResult
            }
        }
    }

    private fun sendLaunchFailureEventAndReturn(
        error: Exception,
        isAppSwitch: Boolean,
        analyticsEventParams: AnalyticsEventParams,
        authorization: Authorization?
    ): PayPalPendingRequest.Failure {
        val errorEvent = if (isAppSwitch) {
            PayPalAnalytics.APP_SWITCH_FAILED
        } else {
            PayPalAnalytics.BROWSER_PRESENTATION_FAILED
        }
        sendEvent(
            eventName = errorEvent,
            params = analyticsEventParams.copy(errorDescription = error.message),
            authorization = authorization
        )
        return PayPalPendingRequest.Failure(error)
    }

    private fun buildLaunchFailureAndReturn(
        errorMessage: String,
        isAppSwitch: Boolean,
        analyticsEventParams: AnalyticsEventParams,
        authorization: Authorization?
    ): PayPalPendingRequest.Failure {
        return sendLaunchFailureEventAndReturn(
            BraintreeException(errorMessage),
            isAppSwitch,
            analyticsEventParams,
            authorization
        )
    }

    private fun processAppSwitchAttempt(
        analyticsEventParams: AnalyticsEventParams,
        authorization: Authorization?
    ): Boolean {
        val attemptAppSwitch = getAppSwitchUseCase() && resolvePayPalUseCase()
        analyticsParamRepository.didSdkAttemptAppSwitch = attemptAppSwitch

        if (attemptAppSwitch) {
            sendEvent(PayPalAnalytics.APP_SWITCH_STARTED, analyticsEventParams, authorization)
        } else {
            sendEvent(PayPalAnalytics.BROWSER_PRESENTATION_STARTED, analyticsEventParams, authorization)
        }

        return attemptAppSwitch
    }

    private fun sendEvent(
        eventName: String,
        params: AnalyticsEventParams = AnalyticsEventParams(),
        authorization: Authorization?
    ) {
        // shortcut: falls back to the newest client's authorization when the pending request's owner
        // is unknown (e.g. after process death); wrong for apps with several authorizations,
        // fix needs the owner carried across process death without persisting a token
        val resolvedAuthorization = authorization ?: merchantRepository.authorizationOrNull ?: return
        analyticsClient.sendEvent(eventName, resolvedAuthorization, params)
    }

    @Throws(BrowserSwitchException::class)
    private fun assertCanPerformBrowserSwitch(
        activity: ComponentActivity,
        params: PayPalPaymentAuthRequestParams
    ) {
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, params.browserSwitchOptions)
    }

    private fun setLaunchAnalyticAttributes(requestParams: PayPalPaymentAuthRequestParams) {
        analyticsParamRepository.fundingSource = requestParams.fundingSource
        analyticsParamRepository.shouldRequestBillingAgreement = requestParams.shouldRequestBillingAgreement
        analyticsParamRepository.recurringBillingPlanType = requestParams.recurringBillingPlanType
    }

    companion object {
        private fun createBrowserSwitchError(exception: BrowserSwitchException): Exception {
            return BraintreeException(
                "AndroidManifest.xml is incorrectly configured or another app defines the same " +
                    "browser switch url as this app. See https://developer.paypal.com/" +
                    "braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup " +
                    "for the correct configuration: " + exception.message
            )
        }
    }
}
