package com.braintreepayments.api;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;

/**
 * Responsible for launching the Venmo app to authenticate users
 */
public class VenmoLauncher {

    @VisibleForTesting
    ActivityResultLauncher<VenmoAuthChallenge> activityLauncher;
    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";

    /**
     * Used to launch the Venmo authentication flow to tokenize a Venmo account. This class must
     * be instantiated in the OnCreate method of your Fragment.
     *
     * @param fragment an Android Fragment from which you will launch the Venmo app
     * @param callback a {@link VenmoAuthChallengeResultCallback} to receive the result of the
     *                 Venmo app switch authentication flow
     */
    public VenmoLauncher(@NonNull Fragment fragment,
                         @NonNull VenmoAuthChallengeResultCallback callback) {
        this(fragment.getActivity().getActivityResultRegistry(), fragment.getViewLifecycleOwner(),
                callback);
    }

    /**
     * Used to launch the Venmo authentication flow to tokenize a Venmo account. This class must
     * be instantiated in the OnCreate method of your Activity.
     *
     * @param activity an Android Activity from which you will launch the Venmo app
     * @param callback a {@link VenmoAuthChallengeResultCallback} to receive the result of the
     *                 Venmo app switch authentication flow
     */
    public VenmoLauncher(@NonNull FragmentActivity activity,
                         @NonNull VenmoAuthChallengeResultCallback callback) {
        this(activity.getActivityResultRegistry(), activity, callback);
    }

    @VisibleForTesting
    VenmoLauncher(ActivityResultRegistry registry, LifecycleOwner lifecycleOwner,
                  VenmoAuthChallengeResultCallback callback) {
        activityLauncher = registry.register(VENMO_SECURE_RESULT, lifecycleOwner,
                new VenmoActivityResultContract(), callback::onVenmoResult);
    }

    /**
     * Launches the Venmo authentication flow by switching to the Venmo app.
     *
     * @param venmoAuthChallenge the result of {@link VenmoClient#requestAuthChallenge(FragmentActivity, VenmoRequest, VenmoAuthChallengeCallback)}
     */
    public void launch(VenmoAuthChallenge venmoAuthChallenge) {
        activityLauncher.launch(venmoAuthChallenge);
    }
}