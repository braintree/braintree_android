package com.braintreepayments.api;

import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
public class GooglePayLifecycleObserver implements LifecycleEventObserver {

    private static final String GOOGLE_PAY_RESULT = "com.braintreepayments.api.GooglePay.RESULT";

    @VisibleForTesting
    GooglePayClient googlePayClient;

    @VisibleForTesting
    ActivityResultRegistry activityResultRegistry;

//    @VisibleForTesting
//    ActivityResultLauncher<> activityLauncher;

    GooglePayLifecycleObserver(ActivityResultRegistry activityResultRegistry, GooglePayClient googlePayClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.googlePayClient = googlePayClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_CREATE) {

        }
    }
}
