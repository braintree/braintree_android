package com.braintreepayments.api;


import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

public class VenmoLauncher {
    ActivityResultLauncher<VenmoAuthChallenge> activityLauncher;
    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";

    public VenmoLauncher(Fragment fragment, VenmoAuthChallengeResultCallback callback) {
        activityLauncher = fragment.getActivity().getActivityResultRegistry().register(VENMO_SECURE_RESULT, fragment.getViewLifecycleOwner(), new VenmoActivityResultContract(), callback::onVenmoResult);
    }

    public void launch(VenmoAuthChallenge venmoAuthChallenge) {
        activityLauncher.launch(venmoAuthChallenge);
    }
}