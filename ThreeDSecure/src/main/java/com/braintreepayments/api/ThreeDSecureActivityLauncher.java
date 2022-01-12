package com.braintreepayments.api;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

class ThreeDSecureActivityLauncher implements DefaultLifecycleObserver {

    private static final String THREED_SECURE_RESULT = "ThreeDSecureResultObserver";

    private ThreeDSecureClient threeDSecureClient;
    private ActivityResultRegistry activityResultRegistry;

    private ActivityResultLauncher<ThreeDSecureResult> activityLauncher;

    ThreeDSecureActivityLauncher(ActivityResultRegistry activityResultRegistry, ThreeDSecureClient threeDSecureClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.threeDSecureClient = threeDSecureClient;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        activityLauncher = activityResultRegistry.register(THREED_SECURE_RESULT, owner, new ThreeDSecureContract(), new ActivityResultCallback<CardinalResult>() {
            @Override
            public void onActivityResult(CardinalResult result) {
                threeDSecureClient.onCardinalResult(result);
            }
        });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        FragmentActivity activity = null;
        if (owner instanceof FragmentActivity) {
            activity = (FragmentActivity) owner;
        } else if (owner instanceof Fragment) {
            activity = ((Fragment) owner).getActivity();
        }

        if (activity != null) {
            threeDSecureClient.deliverBrowserSwitchResult(activity);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        threeDSecureClient = null;
        activityResultRegistry = null;
    }

    void launch(ThreeDSecureResult result) {
        activityLauncher.launch(result);
    }
}
