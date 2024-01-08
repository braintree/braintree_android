package com.braintreepayments.api;


import android.content.Intent;
import android.net.Uri;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

/**
 * Responsible for launching the Venmo app to authenticate users
 */
public class VenmoLauncher {

    @VisibleForTesting
    ActivityResultLauncher<VenmoPaymentAuthRequestParams> activityLauncher;
    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";
    static final String VENMO_PACKAGE_NAME = "com.venmo";

    /**
     * Used to launch the Venmo authentication flow to tokenize a Venmo account. This class must be
     * instantiated before the Fragment is created.
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
     * instantiated before the Activity is created.
     *
     * @param activity an Android Activity from which you will launch the Venmo app
     * @param callback a {@link VenmoLauncherCallback} to receive the result of the Venmo
     *                 app switch authentication flow
     */
    public VenmoLauncher(@NonNull ComponentActivity activity,
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
     * Launches the Venmo authentication flow by switching to the Venmo app. This method cannot be
     * called until the lifecycle of the Fragment or Activity used to instantiate your
     * {@link VenmoLauncher} has reached the CREATED state.
     *
     * @param venmoPaymentAuthRequest the result of
     *                                {@link VenmoClient#createPaymentAuthRequest(android.content.Context, VenmoRequest, VenmoPaymentAuthRequestCallback)}
     */
    public void launch(VenmoPaymentAuthRequest.ReadyToLaunch venmoPaymentAuthRequest) {
        activityLauncher.launch(venmoPaymentAuthRequest.getRequestParams());
    }

    /**
     * Launches an Android Intent pointing to the Venmo app on the Google Play Store
     *
     * @param activity used to open the Venmo's Google Play Store
     */
    public void showVenmoInGooglePlayStore(@NonNull ComponentActivity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
                "https://play.google.com/store/apps/details?id=" + VENMO_PACKAGE_NAME));
        activity.startActivity(intent);
    }
}