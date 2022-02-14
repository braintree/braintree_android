package com.braintreepayments.api;

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
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
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
       if (event == ON_RESUME) {
           FragmentActivity activity = null;
           if (lifecycleOwner instanceof FragmentActivity) {
               activity = (FragmentActivity) lifecycleOwner;
           } else if (lifecycleOwner instanceof Fragment) {
               activity = ((Fragment) lifecycleOwner).getActivity();
           }

           if (activity != null) {
               BrowserSwitchResult pendingResult = payPalClient.getBrowserSwitchResult(activity);
               if (pendingResult != null && pendingResult.getRequestCode() == PAYPAL) {
                   payPalClient.onBrowserSwitchResult(activity);
               }
           }
       }
    }
}
