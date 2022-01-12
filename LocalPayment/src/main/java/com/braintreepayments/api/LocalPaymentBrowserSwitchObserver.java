package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class LocalPaymentBrowserSwitchObserver implements DefaultLifecycleObserver {

    private final LocalPaymentClient localPaymentClient;

    LocalPaymentBrowserSwitchObserver(LocalPaymentClient localPaymentClient) {
        this.localPaymentClient = localPaymentClient;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        FragmentActivity activity = null;
        if (owner instanceof FragmentActivity) {
            activity = (FragmentActivity) owner;
        } else if (owner instanceof Fragment) {
            activity = ((Fragment) owner).getActivity();
        }

        if (activity != null) {
            localPaymentClient.deliverBrowserSwitchResult(activity);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
    }
}
