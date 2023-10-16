package com.braintreepayments.api;

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
        activityLauncher =
                registry.register(THREE_D_SECURE_RESULT, lifecycleOwner,
                        new ThreeDSecureActivityResultContract(),
                        callback::onCardinalResult);
    }

    public void launch(ThreeDSecureResult threeDSecureResult) {
        activityLauncher.launch(threeDSecureResult);
    }
}
