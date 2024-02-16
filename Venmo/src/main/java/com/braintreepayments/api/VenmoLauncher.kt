package com.braintreepayments.api

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity

/**
 * Responsible for launching the Venmo app to authenticate users
 */
class VenmoLauncher internal constructor(
    private val browserSwitchClient: BrowserSwitchClient
) {
    constructor() : this(BrowserSwitchClient())

    /**
     * Launches the Venmo authentication flow by switching to the Venmo app. This method cannot be
     * called until the lifecycle of the Fragment or Activity used to instantiate your
     * [VenmoLauncher] has reached the CREATED state.
     *
     * @param paymentAuthRequest the result of
     * [VenmoClient.createPaymentAuthRequest]
     */
    fun launch(activity: ComponentActivity, paymentAuthRequest: VenmoPaymentAuthRequest.ReadyToLaunch) : VenmoPendingRequest {
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.requestParams)
        } catch (browserSwitchException: BrowserSwitchException) {
            val manifestInvalidError = createBrowserSwitchError(browserSwitchException)
            return VenmoPendingRequest.Failure(manifestInvalidError)
        }
        val request = browserSwitchClient.start(
            activity,
            paymentAuthRequest.requestParams.browserSwitchOptions
        )
        return when (request) {
            is BrowserSwitchPendingRequest.Failure -> VenmoPendingRequest.Failure(request.cause)
            is BrowserSwitchPendingRequest.Started -> VenmoPendingRequest.Started(request)
        }
    }

    fun handleReturnToAppFromBrowser(
        pendingRequest: VenmoPendingRequest.Started,
        intent: Intent
    ): VenmoPaymentAuthResult {
        return when (val browserSwitchResult = browserSwitchClient.parseResult(pendingRequest.request, intent)) {
            is BrowserSwitchResult.Success -> VenmoPaymentAuthResult.Success(
                VenmoPaymentAuthResultInfo(browserSwitchResult.resultInfo)
            )

            is BrowserSwitchResult.NoResult -> VenmoPaymentAuthResult.NoResult
        }
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    fun showVenmoInGooglePlayStore(activity: ComponentActivity) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(
            "https://play.google.com/store/apps/details?id=$VENMO_PACKAGE_NAME"
        )
        activity.startActivity(intent)
    }

    @Throws(BrowserSwitchException::class)
    private fun assertCanPerformBrowserSwitch(
        activity: ComponentActivity,
        params: VenmoPaymentAuthRequestParams
    ) {
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, params.browserSwitchOptions)
    }

    companion object {
        private const val VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT"
        const val VENMO_PACKAGE_NAME = "com.venmo"
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