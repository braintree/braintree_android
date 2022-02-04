package com.braintreepayments.api;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
class ThreeDSecureLifecycleObserver implements LifecycleEventObserver {

    private static final String THREE_D_SECURE_RESULT = "com.braintreepayments.api.ThreeDSecure.RESULT";

    @VisibleForTesting
    ThreeDSecureClient threeDSecureClient;

    @VisibleForTesting
    ActivityResultRegistry activityResultRegistry;

    @VisibleForTesting
    ActivityResultLauncher activityLauncher;

    ThreeDSecureLifecycleObserver(ActivityResultRegistry activityResultRegistry, ThreeDSecureClient threeDSecureClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.threeDSecureClient = threeDSecureClient;
    }

    void launch(ThreeDSecureResult threeDSecureResult) {
        activityLauncher.launch(threeDSecureResult);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                activityLauncher = activityResultRegistry.register(THREE_D_SECURE_RESULT, lifecycleOwner, new ThreeDSecureActivityResultContract(), new ActivityResultCallback<CardinalResult>() {
                    @Override
                    public void onActivityResult(CardinalResult cardinalResult) {
                        threeDSecureClient.onCardinalResult(cardinalResult);
                    }
                });
            case ON_RESUME:
                FragmentActivity activity = null;
                if (lifecycleOwner instanceof FragmentActivity) {
                    activity = (FragmentActivity) lifecycleOwner;
                } else if (lifecycleOwner instanceof Fragment) {
                    activity = ((Fragment) lifecycleOwner).getActivity();
                }

                if (activity != null) {
                    threeDSecureClient.onBrowserSwitchResult(activity);
                }
        }
    }
}
