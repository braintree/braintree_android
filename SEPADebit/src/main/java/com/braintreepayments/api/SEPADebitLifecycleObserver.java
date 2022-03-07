package com.braintreepayments.api;


import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
class SEPADebitLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    SEPADebitClient sepaDebitClient;

    SEPADebitLifecycleObserver(@NonNull SEPADebitClient sepaDebitClient) {
        this.sepaDebitClient = sepaDebitClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            // peek at pending browser switch result to check that request code is SEPA_DEBIT
            // call SEPADebitClient#onBrowserSwitchResult
        }
    }
}
