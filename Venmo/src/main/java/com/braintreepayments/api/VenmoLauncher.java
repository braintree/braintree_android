package com.braintreepayments.api;


import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

public class VenmoLauncher {
    ActivityResultLauncher<VenmoIntentData> activityLauncher;
    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";

    public VenmoLauncher(Fragment fragment, VenmoResultCallback callback) {
        activityLauncher = fragment.getActivity().getActivityResultRegistry().register(VENMO_SECURE_RESULT, fragment.getViewLifecycleOwner(), new VenmoActivityResultContract(), new ActivityResultCallback<VenmoResult>() {
            @Override
            public void onActivityResult(VenmoResult venmoResult) {
                VenmoAccountNonce nonce = new VenmoAccountNonce(venmoResult.getVenmoAccountNonce(), venmoResult.getVenmoUsername(), false);
                callback.onVenmoResult(nonce);
            }
        });
    }

    public void launch(VenmoIntentData venmoIntentData) {
        activityLauncher.launch(venmoIntentData);
    }
}
