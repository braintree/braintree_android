package com.braintreepayments.api;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
class VenmoLifecycleObserver implements LifecycleEventObserver {

    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";

    @VisibleForTesting
    VenmoClient venmoClient;

    @VisibleForTesting
    ActivityResultRegistry activityResultRegistry;

    @VisibleForTesting
    ActivityResultLauncher<VenmoIntentData> activityLauncher;

    VenmoLifecycleObserver(ActivityResultRegistry activityResultRegistry, VenmoClient venmoClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.venmoClient = venmoClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_CREATE) {
            activityLauncher = activityResultRegistry.register(VENMO_SECURE_RESULT, lifecycleOwner, new VenmoActivityResultContract(), new ActivityResultCallback<VenmoResult>() {
                @Override
                public void onActivityResult(VenmoResult venmoResult) {
                    venmoClient.onVenmoResult(venmoResult);
                }
            });
        }
    }

    void launch(VenmoIntentData venmoIntentData) {
        activityLauncher.launch(venmoIntentData);
    }
}
