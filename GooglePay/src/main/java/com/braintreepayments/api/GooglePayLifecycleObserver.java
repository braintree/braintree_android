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
class GooglePayLifecycleObserver implements LifecycleEventObserver {

    private static final String GOOGLE_PAY_RESULT = "com.braintreepayments.api.GooglePay.RESULT";

    @VisibleForTesting
    GooglePayClient googlePayClient;

    @VisibleForTesting
    ActivityResultRegistry activityResultRegistry;

    @VisibleForTesting
    ActivityResultLauncher<GooglePayIntentData> activityLauncher;

    GooglePayLifecycleObserver(ActivityResultRegistry activityResultRegistry, GooglePayClient googlePayClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.googlePayClient = googlePayClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_CREATE) {
            activityLauncher = activityResultRegistry.register(GOOGLE_PAY_RESULT, lifecycleOwner, new GooglePayActivityResultContract(), new ActivityResultCallback<GooglePayResult>() {
                @Override
                public void onActivityResult(GooglePayResult googlePayResult) {
                    googlePayClient.onGooglePayResult(googlePayResult);
                }
            });
        }
    }

    void launch(GooglePayIntentData googlePayIntentData) {
        activityLauncher.launch(googlePayIntentData);
    }
}
