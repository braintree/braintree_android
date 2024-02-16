package com.braintreepayments.api

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

/**
 * Responsible for launching the Venmo app to authenticate users
 */
class VenmoLauncher @VisibleForTesting internal constructor(
    registry: ActivityResultRegistry, lifecycleOwner: LifecycleOwner?,
    callback: VenmoLauncherCallback
) {
    @JvmField
    @VisibleForTesting
    var activityLauncher: ActivityResultLauncher<VenmoPaymentAuthRequestParams>

    /**
     * Used to launch the Venmo authentication flow to tokenize a Venmo account. This class must be
     * instantiated before the Fragment is created.
     *
     * @param fragment an Android Fragment from which you will launch the Venmo app
     * @param callback a [VenmoLauncherCallback] to receive the result of the Venmo
     * app switch authentication flow
     */
    constructor(
        fragment: Fragment,
        callback: VenmoLauncherCallback
    ) : this(
        fragment.requireActivity().activityResultRegistry, fragment.viewLifecycleOwner,
        callback
    )

    /**
     * Used to launch the Venmo authentication flow to tokenize a Venmo account. This class must be
     * instantiated before the Activity is created.
     *
     * @param activity an Android Activity from which you will launch the Venmo app
     * @param callback a [VenmoLauncherCallback] to receive the result of the Venmo
     * app switch authentication flow
     */
    constructor(
        activity: ComponentActivity,
        callback: VenmoLauncherCallback
    ) : this(activity.activityResultRegistry, activity, callback)

    init {
        activityLauncher = registry.register(
            VENMO_SECURE_RESULT, lifecycleOwner!!,
            VenmoActivityResultContract()
        ) { venmoPaymentAuthResult: VenmoPaymentAuthResult? ->
            callback.onVenmoPaymentAuthResult(
                venmoPaymentAuthResult!!
            )
        }
    }

    /**
     * Launches the Venmo authentication flow by switching to the Venmo app. This method cannot be
     * called until the lifecycle of the Fragment or Activity used to instantiate your
     * [VenmoLauncher] has reached the CREATED state.
     *
     * @param venmoPaymentAuthRequest the result of
     * [VenmoClient.createPaymentAuthRequest]
     */
    fun launch(venmoPaymentAuthRequest: VenmoPaymentAuthRequest.ReadyToLaunch) {
        activityLauncher.launch(venmoPaymentAuthRequest.requestParams)
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

    companion object {
        private const val VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT"
        const val VENMO_PACKAGE_NAME = "com.venmo"
    }
}