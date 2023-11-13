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
    ActivityResultLauncher<VenmoPaymentAuthRequestParams> activityLauncher;
    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";

    /**
     * Used to launch the Venmo authentication flow to tokenize a Venmo account. This class must be
     * instantiated in the OnCreate method of your Fragment.
     *
     * @param fragment an Android Fragment from which you will launch the Venmo app
     * @param callback a {@link VenmoLauncherCallback} to receive the result of the Venmo
     *                 app switch authentication flow
     */
    public VenmoLauncher(@NonNull Fragment fragment,
                         @NonNull VenmoLauncherCallback callback) {
        this(fragment.getActivity().getActivityResultRegistry(), fragment.getViewLifecycleOwner(),
                callback);
    }

    /**
     * Used to launch the Venmo authentication flow to tokenize a Venmo account. This class must be
     * instantiated in the OnCreate method of your Activity.
     *
     * @param activity an Android Activity from which you will launch the Venmo app
     * @param callback a {@link VenmoLauncherCallback} to receive the result of the Venmo
     *                 app switch authentication flow
     */
    public VenmoLauncher(@NonNull FragmentActivity activity,
                         @NonNull VenmoLauncherCallback callback) {
        this(activity.getActivityResultRegistry(), activity, callback);
    }

    @VisibleForTesting
    VenmoLauncher(ActivityResultRegistry registry, LifecycleOwner lifecycleOwner,
                  VenmoLauncherCallback callback) {
        activityLauncher = registry.register(VENMO_SECURE_RESULT, lifecycleOwner,
                new VenmoActivityResultContract(), callback::onVenmoPaymentAuthResult);
    }

    /**
     * Launches the Venmo authentication flow by switching to the Venmo app.
     *
     * @param venmoPaymentAuthRequest the result of
     *                                {@link VenmoClient#createPaymentAuthRequest(FragmentActivity,
     *                                VenmoRequest, VenmoPaymentAuthRequestCallback)}
     */
    public void launch(VenmoPaymentAuthRequest.ReadyToLaunch venmoPaymentAuthRequest) {
        activityLauncher.launch(venmoPaymentAuthRequest.getRequestParams());
    }
}