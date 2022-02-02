package com.braintreepayments.api;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

class ThreeDSecureLifecycleObserver implements LifecycleEventObserver {

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

    void launch(ThreeDSecureResult threeDSecureResult) {
        activityLauncher.launch(threeDSecureResult);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        switch(event) {
            case ON_RESUME:
        }
    }
}
