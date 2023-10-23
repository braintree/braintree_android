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
    ActivityResultLauncher<ThreeDSecureResult> activityLauncher;
    private final CardinalResultCallback callback;

    /**
     * Used to launch the 3DS authentication flow to tokenize a 3DS card. This class must be
     * instantiated in the onCreateView method of your Fragment
     *
     * @param fragment an Android Fragment from which you will launch the 3DS flow
     * @param callback a {@link CardinalResultCallback} to received the result of the 3DS
     *                 authentication flow
     */
    public ThreeDSecureLauncher(@NonNull Fragment fragment,
                                @NonNull CardinalResultCallback callback) {
        this(fragment.getActivity().getActivityResultRegistry(), fragment.getViewLifecycleOwner(),
                callback);
    }

    /**
     * Used to launch the 3DS authentication flow to tokenize a 3DS card. This class must be
     * instantiated in the onCreate method of your FragmentActivity
     *
     * @param activity an Android Activity from which you will launch the 3DS flow
     * @param callback a {@link CardinalResultCallback} to received the result of the 3DS
     *                 authentication flow
     */
    public ThreeDSecureLauncher(@NonNull FragmentActivity activity,
                                @NonNull CardinalResultCallback callback) {
        this(activity.getActivityResultRegistry(), activity, callback);
    }

    @VisibleForTesting
    ThreeDSecureLauncher(ActivityResultRegistry registry, LifecycleOwner lifecycleOwner,
                         CardinalResultCallback callback) {
        this.callback = callback;
        activityLauncher =
                registry.register(THREE_D_SECURE_RESULT, lifecycleOwner,
                        new ThreeDSecureActivityResultContract(),
                        callback::onCardinalResult);
    }

    /**
     * Launches the 3DS flow by switching to an authentication Activity. Call this method in the
     * callback of
     * {@link ThreeDSecureClient#performVerification(Context, ThreeDSecureRequest,
     * ThreeDSecureResultCallback)} if user authentication is required
     * {@link ThreeDSecureLookup#requiresUserAuthentication()}
     *
     * @param threeDSecureResult the result of
     *                           {@link
     *                           ThreeDSecureClient#continuePerformVerification(ThreeDSecureResult,
     *                           ThreeDSecureResultCallback)}
     */
    public void launch(ThreeDSecureResult threeDSecureResult) {
        try {
            activityLauncher.launch(threeDSecureResult);
        } catch (RuntimeException runtimeException) {
            Throwable exceptionCause = runtimeException.getCause();
            if (exceptionCause instanceof TransactionTooLargeException) {
                String errorMessage = "The 3D Secure response returned is too large to continue. "
                        + "Please contact Braintree Support for assistance.";
                BraintreeException threeDSecureResponseTooLargeError =
                        new BraintreeException(errorMessage, runtimeException);
                callback.onCardinalResult(new CardinalResult(threeDSecureResponseTooLargeError));
            } else {
                throw runtimeException;
            }
        }
    }
}
