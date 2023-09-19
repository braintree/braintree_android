package com.braintreepayments.api;


import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class VenmoLauncher {
    ActivityResultLauncher<VenmoIntentData> activityLauncher;
    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";

    public VenmoLauncher(Fragment fragment, VenmoResultCallback callback) {
        activityLauncher = fragment.getActivity().getActivityResultRegistry().register(VENMO_SECURE_RESULT, fragment.getViewLifecycleOwner(), new VenmoActivityResultContract(), callback::onVenmoResult);
    }

    public void launch(VenmoIntentData venmoIntentData) {
        activityLauncher.launch(venmoIntentData);
    }
}
