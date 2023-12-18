package com.braintreepayments.api;

import android.content.Context;
import android.os.TransactionTooLargeException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

/**
 * Launcher for the app-based authentication challenge for 3D secure tokenization.
 */
public class ThreeDSecureLauncher {

    private static final String THREE_D_SECURE_RESULT =
            "com.braintreepayments.api.ThreeDSecure.RESULT";
    @VisibleForTesting
    ActivityResultLauncher<ThreeDSecureParams> activityLauncher;
    private final ThreeDSecureLauncherCallback callback;

    /**
     * Used to launch the 3DS authentication flow to tokenize a 3DS card. This class must be
     * instantiated in the onCreateView method of your Fragment
     *
     * @param fragment an Android Fragment from which you will launch the 3DS flow
     * @param callback a {@link ThreeDSecureLauncherCallback} to received the result of the 3DS
     *                 authentication flow
     */
    public ThreeDSecureLauncher(@NonNull Fragment fragment,
                                @NonNull ThreeDSecureLauncherCallback callback) {
        this(fragment.getActivity().getActivityResultRegistry(), fragment.getViewLifecycleOwner(),
                callback);
    }

    /**
     * Used to launch the 3DS authentication flow to tokenize a 3DS card. This class must be
     * instantiated in the onCreate method of your FragmentActivity
     *
     * @param activity an Android Activity from which you will launch the 3DS flow
     * @param callback a {@link ThreeDSecureLauncherCallback} to received the result of the 3DS
     *                 authentication flow
     */
    public ThreeDSecureLauncher(@NonNull FragmentActivity activity,
                                @NonNull ThreeDSecureLauncherCallback callback) {
        this(activity.getActivityResultRegistry(), activity, callback);
    }

    @VisibleForTesting
    ThreeDSecureLauncher(ActivityResultRegistry registry, LifecycleOwner lifecycleOwner,
                         ThreeDSecureLauncherCallback callback) {
        this.callback = callback;
        activityLauncher =
                registry.register(THREE_D_SECURE_RESULT, lifecycleOwner,
                        new ThreeDSecureActivityResultContract(),
                        callback::onThreeDSecurePaymentAuthResult);
    }

    /**
     * Launches the 3DS flow by switching to an authentication Activity. Call this method in the
     * callback of
     * {@link ThreeDSecureClient#createPaymentAuthRequest(Context, ThreeDSecureRequest,
     * ThreeDSecurePaymentAuthRequestCallback)} if user authentication is required
     * {@link ThreeDSecureLookup#requiresUserAuthentication()}
     *
     * @param paymentAuthRequest the result of
     *                           {@link
     *                           ThreeDSecureClient#continuePerformVerification(ThreeDSecureParams,
     *                           ThreeDSecureResultCallback)}
     */
    public void launch(ThreeDSecurePaymentAuthRequest.ReadyToLaunch paymentAuthRequest) {
        try {
            activityLauncher.launch(paymentAuthRequest.getRequestParams());
        } catch (RuntimeException runtimeException) {
            Throwable exceptionCause = runtimeException.getCause();
            if (exceptionCause instanceof TransactionTooLargeException) {
                String errorMessage = "The 3D Secure response returned is too large to continue. "
                        + "Please contact Braintree Support for assistance.";
                BraintreeException threeDSecureResponseTooLargeError =
                        new BraintreeException(errorMessage, runtimeException);
                callback.onThreeDSecurePaymentAuthResult(new ThreeDSecurePaymentAuthResult(threeDSecureResponseTooLargeError));
            } else {
                throw runtimeException;
            }
        }
    }
}
