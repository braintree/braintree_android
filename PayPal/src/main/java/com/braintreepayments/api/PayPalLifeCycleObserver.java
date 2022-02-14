package com.braintreepayments.api;

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

class PayPalLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    final PayPalClient payPalClient;

    PayPalLifecycleObserver(PayPalClient payPalClient) {
        this.payPalClient = payPalClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
       if (event == ON_RESUME) {
           // TODO: implement
       }
    }
}
