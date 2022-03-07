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
class SEPADebitLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    SEPADebitClient sepaDebitClient;

    SEPADebitLifecycleObserver(@NonNull SEPADebitClient sepaDebitClient) {
        this.sepaDebitClient = sepaDebitClient;
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
                BrowserSwitchResult pendingResult = sepaDebitClient.getBrowserSwitchResult(activity);
                if (pendingResult != null && pendingResult.getRequestCode() == BraintreeRequestCodes.SEPA) {
                    sepaDebitClient.onBrowserSwitchResult(activity);
                }
            }
        }
    }
}
