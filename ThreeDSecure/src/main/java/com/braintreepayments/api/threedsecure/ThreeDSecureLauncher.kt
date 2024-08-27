package com.braintreepayments.api.threedsecure

import android.os.TransactionTooLargeException
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.braintreepayments.api.core.BraintreeException

/**
 * Launcher for the app-based authentication challenge for 3D secure tokenization.
 */
class ThreeDSecureLauncher internal constructor(
    registry: ActivityResultRegistry,
    lifecycleOwner: LifecycleOwner,
    private val callback: ThreeDSecureLauncherCallback
) {

    var activityLauncher: ActivityResultLauncher<ThreeDSecureParams?> = registry.register(
        THREE_D_SECURE_RESULT,
        lifecycleOwner,
        ThreeDSecureActivityResultContract()
    ) { paymentAuthResult: ThreeDSecurePaymentAuthResult? ->
        if (paymentAuthResult != null) {
            callback.onThreeDSecurePaymentAuthResult(paymentAuthResult)
        } else {
            callback.onThreeDSecurePaymentAuthResult(
                ThreeDSecurePaymentAuthResult(
                    error = BraintreeException("Payment Auth Result is null")
                )
            )
        }
    }

    /**
     * Used to launch the 3DS authentication flow to tokenize a 3DS card. This class must be
     * instantiated before your Fragment is created.
     *
     * @param fragment an Android Fragment from which you will launch the 3DS flow
     * @param callback a [ThreeDSecureLauncherCallback] to received the result of the 3DS
     * authentication flow
     */
    constructor(
        fragment: Fragment,
        callback: ThreeDSecureLauncherCallback
    ) : this(
        registry = fragment.requireActivity().activityResultRegistry,
        lifecycleOwner = fragment.viewLifecycleOwner,
        callback = callback
    )

    /**
     * Used to launch the 3DS authentication flow to tokenize a 3DS card. This class must be
     * instantiated before your Activity is created.
     *
     * @param activity an Android Activity from which you will launch the 3DS flow
     * @param callback a [ThreeDSecureLauncherCallback] to received the result of the 3DS
     * authentication flow
     */
    constructor(
        activity: ComponentActivity,
        callback: ThreeDSecureLauncherCallback
    ) : this(activity.activityResultRegistry, activity, callback)

    /**
     * Launches the 3DS flow by switching to an authentication Activity. Call this method in the
     * callback of [ThreeDSecureClient.createPaymentAuthRequest] if user authentication is required
     * [ThreeDSecureLookup.requiresUserAuthentication]. This method cannot be called until
     * the lifecycle of the Fragment or Activity used to instantiate your [ThreeDSecureLauncher]
     * has reached the CREATED state.
     *
     * @param paymentAuthRequest the result of [ThreeDSecureClient.createPaymentAuthRequest]
     */
    @Suppress("TooGenericExceptionCaught")
    fun launch(paymentAuthRequest: ThreeDSecurePaymentAuthRequest.ReadyToLaunch) {
        try {
            activityLauncher.launch(paymentAuthRequest.requestParams)
        } catch (runtimeException: RuntimeException) {
            if (runtimeException.cause is TransactionTooLargeException) {
                val threeDSecureResponseTooLargeError =
                    BraintreeException(
                        message = "The 3D Secure response returned is too large to continue. " +
                            "Please contact Braintree Support for assistance.",
                        cause = runtimeException
                    )
                callback.onThreeDSecurePaymentAuthResult(
                    ThreeDSecurePaymentAuthResult(
                        error = threeDSecureResponseTooLargeError
                    )
                )
            } else {
                throw runtimeException
            }
        }
    }

    companion object {
        private const val THREE_D_SECURE_RESULT = "com.braintreepayments.api.ThreeDSecure.RESULT"
    }
}
