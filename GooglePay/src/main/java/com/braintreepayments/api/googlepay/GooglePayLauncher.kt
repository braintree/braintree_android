package com.braintreepayments.api.googlepay

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

/**
 * Responsible for launching the Google Pay payment sheet
 */
class GooglePayLauncher @VisibleForTesting internal constructor(
    registry: ActivityResultRegistry, lifecycleOwner: LifecycleOwner?,
    callback: GooglePayLauncherCallback
) {
    @VisibleForTesting
    var activityLauncher: ActivityResultLauncher<GooglePayPaymentAuthRequestParams> = registry.register(
        GOOGLE_PAY_RESULT, lifecycleOwner!!,
        GooglePayActivityResultContract()
    ) { googlePayPaymentAuthResult: GooglePayPaymentAuthResult? ->
        callback.onGooglePayLauncherResult(
            googlePayPaymentAuthResult
        )
    }

    /**
     * Used to launch the Google Pay payment sheet from within an Android Fragment. This class must be
     * instantiated before the Fragment is created.
     *
     * @param fragment the Android Fragment from which you will launch the Google Pay payment sheet
     * @param callback a [GooglePayLauncherCallback] to receive the result of the Google Pay
     * payment flow
     */
    constructor(
        fragment: Fragment,
        callback: GooglePayLauncherCallback
    ) : this(
        fragment.requireActivity().activityResultRegistry, fragment.viewLifecycleOwner,
        callback
    )

    /**
     * Used to launch the Google Pay payment sheet from within an Android Activity. This class must be
     * instantiated before the Activity is created.
     *
     * @param activity the Android Activity from which you will launch the Google Pay payment sheet
     * @param callback a [GooglePayLauncherCallback] to receive the result of the Google Pay
     * payment flow
     */
    constructor(
        activity: ComponentActivity,
        callback: GooglePayLauncherCallback
    ) : this(activity.activityResultRegistry, activity, callback)

    /**
     * Launches the Google Pay payment sheet. This method cannot be called until the lifecycle of
     * the Fragment or Activity used to instantiate your [GooglePayLauncher] has reached the
     * CREATED state.
     *
     * @param googlePayPaymentAuthRequestParams the [GooglePayPaymentAuthRequestParams]
     * received from invoking
     * [GooglePayClient.createPaymentAuthRequest]
     */
    fun launch(googlePayPaymentAuthRequestParams: GooglePayPaymentAuthRequestParams) {
        activityLauncher.launch(googlePayPaymentAuthRequestParams)
    }

    companion object {
        private const val GOOGLE_PAY_RESULT = "com.braintreepayments.api.GooglePay.RESULT"
    }
}
