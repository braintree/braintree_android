package com.braintreepayments.api;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class VenmoLauncher implements DefaultLifecycleObserver {

    private static final String VENMO_RESULT = "VenmoObserver";

    private VenmoClient venmoClient;
    private ActivityResultRegistry activityResultRegistry;

    private ActivityResultLauncher<VenmoContractInput> activityLauncher;

    VenmoLauncher(ActivityResultRegistry activityResultRegistry, VenmoClient venmoClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.venmoClient = venmoClient;
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        activityLauncher = activityResultRegistry.register(VENMO_RESULT, owner, new VenmoContract(), new ActivityResultCallback<VenmoResult>() {
            @Override
            public void onActivityResult(VenmoResult result) {
                venmoClient.onVenmoResult(result);
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
        venmoClient = null;
        activityResultRegistry = null;
    }

    void launch(VenmoContractInput result) {
        activityLauncher.launch(result);
    }
}
