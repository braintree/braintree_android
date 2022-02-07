package com.braintreepayments.api;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

class VenmoLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    VenmoClient venmoClient;

    @VisibleForTesting
    ActivityResultRegistry activityResultRegistry;

    @VisibleForTesting
    ActivityResultLauncher activityLauncher;

    VenmoLifecycleObserver(ActivityResultRegistry activityResultRegistry, VenmoClient venmoClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.venmoClient = venmoClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_CREATE) {
        }
    }
}
