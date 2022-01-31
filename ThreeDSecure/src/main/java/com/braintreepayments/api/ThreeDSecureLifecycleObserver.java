package com.braintreepayments.api;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

class ThreeDSecureLifecycleObserver implements DefaultLifecycleObserver {

    private static final String THREED_SECURE_RESULT = "com.braintreepayments.api.ThreeDSecure.RESULT";

    private final ThreeDSecureClient threeDSecureClient;
    private final ActivityResultRegistry activityResultRegistry;

    private ActivityResultLauncher<ThreeDSecureResult> activityLauncher;

    ThreeDSecureLifecycleObserver(ActivityResultRegistry activityResultRegistry, ThreeDSecureClient threeDSecureClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.threeDSecureClient = threeDSecureClient;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        activityLauncher = activityResultRegistry.register(THREED_SECURE_RESULT, owner, new ThreeDSecureActivityResultContract(), new ActivityResultCallback<CardinalResult>() {
            @Override
            public void onActivityResult(CardinalResult cardinalResult) {
                threeDSecureClient.onCardinalResult(cardinalResult);
            }
        });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
    }

    void launch(ThreeDSecureResult threeDSecureResult) {
        activityLauncher.launch(threeDSecureResult);
    }
}
