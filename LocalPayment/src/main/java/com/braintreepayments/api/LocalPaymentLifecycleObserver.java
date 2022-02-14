package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

class LocalPaymentLifecycleObserver implements LifecycleEventObserver {

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            // TODO: implement
        }
    }
}
