package com.braintreepayments.api.googlepay

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.braintreepayments.api.core.UserCanceledException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.google.android.gms.wallet.contract.TaskResultContracts

/**
 * Responsible for launching the Google Pay payment sheet
 */
class GooglePayLauncher internal constructor(
    registry: ActivityResultRegistry,
    lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val internalGooglePayClient: GooglePayInternalClient = GooglePayInternalClient(),
    callback: GooglePayLauncherCallback
) {

    private val activityLauncher: ActivityResultLauncher<Task<PaymentData>> = registry.register(
        GOOGLE_PAY_RESULT, lifecycleOwner,
        TaskResultContracts.GetPaymentDataResult()
    ) { apiTaskResult: ApiTaskResult<PaymentData> ->
        val result = when {
            apiTaskResult.status.isSuccess ->
                GooglePayPaymentAuthResult(apiTaskResult.result, null)
            apiTaskResult.status.isCanceled ->
                GooglePayPaymentAuthResult(null, UserCanceledException("User canceled Google Pay."))
            else ->
                GooglePayPaymentAuthResult(
                    null,
                    GooglePayException(
                        "An error was encountered during the Google Pay " +
                                "flow. See the status object in this exception for more details.",
                        apiTaskResult.status
                    )
                )
        }
        callback.onGooglePayLauncherResult(result)
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
        fragment.requireActivity().activityResultRegistry,
        fragment.viewLifecycleOwner,
        fragment.requireContext(),
        callback = callback
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
    ) : this(activity.activityResultRegistry, activity, activity, callback = callback)

    /**
     * Launches the Google Pay payment sheet. This method cannot be called until the lifecycle of
     * the Fragment or Activity used to instantiate your [GooglePayLauncher] has reached the
     * CREATED state.
     *
     * @param paymentAuthRequest the [GooglePayPaymentAuthRequestParams]
     * received from invoking [GooglePayClient.createPaymentAuthRequest]
     */
    fun launch(paymentAuthRequest: GooglePayPaymentAuthRequest.ReadyToLaunch) {
        internalGooglePayClient.loadPaymentData(context, paymentAuthRequest.requestParams)
            .addOnCompleteListener { completedTask ->
                activityLauncher.launch(completedTask)
            }
    }

    companion object {
        private const val GOOGLE_PAY_RESULT = "com.braintreepayments.api.GooglePay.RESULT"
    }
}
