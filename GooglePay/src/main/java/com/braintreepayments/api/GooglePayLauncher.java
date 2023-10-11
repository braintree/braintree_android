package com.braintreepayments.api;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

/**
 * Responsible for launching the Google Pay dialog
 */
public class GooglePayLauncher {

    @VisibleForTesting
    ActivityResultLauncher<GooglePayIntentData> activityLauncher;

    private static final String GOOGLE_PAY_RESULT = "com.braintreepayments.api.GooglePay.RESULT";

    public GooglePayLauncher(@NonNull Fragment fragment,
                             @NonNull GooglePayResultCallback callback) {
        this(fragment.getActivity().getActivityResultRegistry(), fragment.getViewLifecycleOwner(), callback);
    }

    public GooglePayLauncher(@NonNull FragmentActivity activity,
                         @NonNull GooglePayResultCallback callback) {
        this(activity.getActivityResultRegistry(), activity, callback);
    }

    @VisibleForTesting
    GooglePayLauncher(ActivityResultRegistry registry, LifecycleOwner lifecycleOwner,
                      GooglePayResultCallback callback) {
        activityLauncher = registry.register(GOOGLE_PAY_RESULT, lifecycleOwner,
                new GooglePayActivityResultContract(), callback::onGooglePayResult);
    }
}
