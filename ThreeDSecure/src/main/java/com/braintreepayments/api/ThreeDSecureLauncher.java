package com.braintreepayments.api;

import android.os.TransactionTooLargeException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

public class ThreeDSecureLauncher {

    private static final String THREE_D_SECURE_RESULT =
            "com.braintreepayments.api.ThreeDSecure.RESULT";
    @VisibleForTesting
    ActivityResultLauncher<ThreeDSecureResult> activityLauncher;
    private CardinalResultCallback callback;

    public ThreeDSecureLauncher(@NonNull Fragment fragment,
                                @NonNull CardinalResultCallback callback) {
        this(fragment.getActivity().getActivityResultRegistry(), fragment.getViewLifecycleOwner(),
                callback);
    }

    public ThreeDSecureLauncher(@NonNull FragmentActivity activity,
                                @NonNull CardinalResultCallback callback) {
        this(activity.getActivityResultRegistry(), activity, callback);
    }

    public ThreeDSecureLauncher(ActivityResultRegistry registry, LifecycleOwner lifecycleOwner,
                                CardinalResultCallback callback) {
        this.callback = callback;
        activityLauncher =
                registry.register(THREE_D_SECURE_RESULT, lifecycleOwner,
                        new ThreeDSecureActivityResultContract(),
                        callback::onCardinalResult);
    }

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
