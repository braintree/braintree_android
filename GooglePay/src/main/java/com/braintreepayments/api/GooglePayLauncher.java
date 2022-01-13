package com.braintreepayments.api;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class GooglePayLauncher implements DefaultLifecycleObserver {

    private static final String GOOGLE_PAY_RESULT = "GooglePayResult";

    private GooglePayClient googlePayClient;
    private ActivityResultRegistry activityResultRegistry;

    private ActivityResultLauncher<GooglePayContractInput> activityLauncher;

    GooglePayLauncher(ActivityResultRegistry activityResultRegistry, GooglePayClient googlePayClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.googlePayClient = googlePayClient;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        activityLauncher = activityResultRegistry.register(GOOGLE_PAY_RESULT, owner, new GooglePayContract(), new ActivityResultCallback<GooglePayResult>() {
            @Override
            public void onActivityResult(GooglePayResult result) {
                googlePayClient.onGooglePayResult(result);
            }
        });
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        googlePayClient = null;
        activityResultRegistry = null;
    }

    void launch(GooglePayContractInput result) {
        activityLauncher.launch(result);
    }
}
