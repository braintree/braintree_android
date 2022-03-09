package com.braintreepayments.api;


import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
class SEPADirectDebitLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    SEPADirectDebitClient sepaDirectDebitClient;

    SEPADirectDebitLifecycleObserver(@NonNull SEPADirectDebitClient sepaDirectDebitClient) {
        this.sepaDirectDebitClient = sepaDirectDebitClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == ON_RESUME) {
            FragmentActivity activity = null;
            if (lifecycleOwner instanceof FragmentActivity) {
                activity = (FragmentActivity) lifecycleOwner;
            } else if (lifecycleOwner instanceof Fragment) {
                activity = ((Fragment) lifecycleOwner).getActivity();
            }

            if (activity != null) {
                BrowserSwitchResult pendingResult = sepaDirectDebitClient.getBrowserSwitchResult(activity);
                if (pendingResult != null && pendingResult.getRequestCode() == BraintreeRequestCodes.SEPA_DEBIT) {
                    sepaDirectDebitClient.onBrowserSwitchResult(activity);
                }
            }
        }
    }
}
