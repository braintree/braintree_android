package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

class LocalPaymentLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    LocalPaymentClient localPaymentClient;

    LocalPaymentLifecycleObserver(@NonNull LocalPaymentClient localPaymentClient) {
        this.localPaymentClient = localPaymentClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            // TODO: implement
        }
    }
}
