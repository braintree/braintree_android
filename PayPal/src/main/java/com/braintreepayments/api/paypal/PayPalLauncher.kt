package com.braintreepayments.api.paypal

import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AppSwitchRepository
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GetAppSwitchUseCase
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.MerchantRepository

/**
 * Responsible for launching PayPal user authentication in a web browser
 */
class PayPalLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val getReturnLinkUseCase: GetReturnLinkUseCase = GetReturnLinkUseCase(merchantRepository),
    private val getAppSwitchUseCase: GetAppSwitchUseCase = GetAppSwitchUseCase(AppSwitchRepository.instance),
    lazyAnalyticsClient: Lazy<AnalyticsClient>
) {
    /**
     * Used to launch the PayPal flow in a web browser and deliver results to your Activity
     */
    constructor() : this(
        browserSwitchClient = BrowserSwitchClient(),
        lazyAnalyticsClient = AnalyticsClient.lazyInstance
    )

    private val analyticsClient: AnalyticsClient by lazyAnalyticsClient

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
        val isAppSwitch = getAppSwitchUseCase()
        val paypalContextId = paymentAuthRequest.requestParams.paypalContextId
        val appSwitchReturnUrl = (getReturnLinkUseCase() as? GetReturnLinkUseCase.ReturnLinkResult.AppLink)
            ?.appLinkReturnUri?.toString()
        val analyticsEventParams = AnalyticsEventParams(
            payPalContextId = paypalContextId,
            appSwitchUrl = appSwitchReturnUrl
        )

        analyticsClient.sendEvent(PayPalAnalytics.APP_SWITCH_STARTED, analyticsEventParams)

        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.requestParams)
        } catch (browserSwitchException: BrowserSwitchException) {
            val manifestInvalidError = createBrowserSwitchError(browserSwitchException)
            return sendLaunchFailureEventAndReturn(manifestInvalidError, isAppSwitch, analyticsEventParams)
        }

        val options = paymentAuthRequest.requestParams.browserSwitchOptions
        if (options == null) {
            val error = BraintreeException("BrowserSwitchOptions is null")
            return sendLaunchFailureEventAndReturn(error, isAppSwitch, analyticsEventParams)
        }

        return when (val request = browserSwitchClient.start(activity, options)) {
            is BrowserSwitchStartResult.Failure -> {
                val errorEvent = if (isAppSwitch) {
                    PayPalAnalytics.APP_SWITCH_FAILED
                } else {
                    PayPalAnalytics.BROWSER_PRESENTATION_FAILED
                }
                analyticsClient.sendEvent(
                    eventName = errorEvent,
                    analyticsEventParams = analyticsEventParams.copy(errorDescription = request.error.message)
                )
                PayPalPendingRequest.Failure(request.error)
            }

            is BrowserSwitchStartResult.Started -> {
                val event = if (isAppSwitch) {
                    PayPalAnalytics.APP_SWITCH_SUCCEEDED
                } else {
                    PayPalAnalytics.BROWSER_PRESENTATION_SUCCEEDED
                }
                analyticsClient.sendEvent(
                    eventName = event,
                    analyticsEventParams = analyticsEventParams
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
        val appSwitchUrl = when (val returnLinkResult = getReturnLinkUseCase()) {
            is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> {
                returnLinkResult.appLinkReturnUri.toString()
            }

            is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> {
                returnLinkResult.deepLinkFallbackUrlScheme
            }

            else -> null
        }
        val payPalContextId = intent.data?.getQueryParameter("ba_token") ?: intent.data?.getQueryParameter("token")
        val analyticsEventParams = AnalyticsEventParams(
            payPalContextId = payPalContextId,
            appSwitchUrl = appSwitchUrl
        )

        analyticsClient.sendEvent(PayPalAnalytics.HANDLE_RETURN_STARTED, analyticsEventParams)

        return when (
            val browserSwitchResult = browserSwitchClient.completeRequest(intent, pendingRequest.pendingRequestString)
        ) {
            is BrowserSwitchFinalResult.Success -> {
                analyticsClient.sendEvent(PayPalAnalytics.HANDLE_RETURN_SUCCEEDED, analyticsEventParams)
                PayPalPaymentAuthResult.Success(browserSwitchResult)
            }

            is BrowserSwitchFinalResult.Failure -> {
                analyticsClient.sendEvent(
                    PayPalAnalytics.HANDLE_RETURN_FAILED,
                    analyticsEventParams.copy(
                        errorDescription = browserSwitchResult.error.message
                    )
                )
                PayPalPaymentAuthResult.Failure(browserSwitchResult.error)
            }

            is BrowserSwitchFinalResult.NoResult -> {
                analyticsClient.sendEvent(PayPalAnalytics.HANDLE_RETURN_NO_RESULT, analyticsEventParams)
                PayPalPaymentAuthResult.NoResult
            }
        }
    }

    private fun sendLaunchFailureEventAndReturn(
        error: Exception,
        isAppSwitch: Boolean,
        analyticsEventParams: AnalyticsEventParams
    ): PayPalPendingRequest.Failure {
        val errorEvent = if (isAppSwitch) {
            PayPalAnalytics.APP_SWITCH_FAILED
        } else {
            PayPalAnalytics.BROWSER_PRESENTATION_FAILED
        }
        analyticsClient.sendEvent(
            eventName = errorEvent,
            analyticsEventParams = analyticsEventParams.copy(errorDescription = error.message)
        )
        return PayPalPendingRequest.Failure(error)
    }

    @Throws(BrowserSwitchException::class)
    private fun assertCanPerformBrowserSwitch(
        activity: ComponentActivity,
        params: PayPalPaymentAuthRequestParams
    ) {
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, params.browserSwitchOptions)
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
