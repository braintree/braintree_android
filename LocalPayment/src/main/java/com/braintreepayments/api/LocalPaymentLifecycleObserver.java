package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.LOCAL_PAYMENT;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
class LocalPaymentLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    LocalPaymentClient localPaymentClient;

    LocalPaymentLifecycleObserver(@NonNull LocalPaymentClient localPaymentClient) {
        this.localPaymentClient = localPaymentClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            FragmentActivity activity = null;
            if (lifecycleOwner instanceof FragmentActivity) {
                activity = (FragmentActivity) lifecycleOwner;
            } else if (lifecycleOwner instanceof Fragment) {
                activity = ((Fragment) lifecycleOwner).getActivity();
            }

            if (activity != null) {
                BrowserSwitchResult pendingResult = localPaymentClient.getBrowserSwitchResult(activity);
                if (pendingResult != null && pendingResult.getRequestCode() == LOCAL_PAYMENT) {
                    localPaymentClient.onBrowserSwitchResult(activity);
                }
            }
        }
    }
}
