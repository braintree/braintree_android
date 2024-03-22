package com.braintreepayments.api.googlepay;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

/**
 * Responsible for launching the Google Pay payment sheet
 */
public class GooglePayLauncher {

    @VisibleForTesting
    ActivityResultLauncher<GooglePayPaymentAuthRequestParams> activityLauncher;

    private static final String GOOGLE_PAY_RESULT = "com.braintreepayments.api.GooglePay.RESULT";

    /**
     * Used to launch the Google Pay payment sheet from within an Android Fragment. This class must be
     * instantiated before the Fragment is created.
     *
     * @param fragment the Android Fragment from which you will launch the Google Pay payment sheet
     * @param callback a {@link GooglePayLauncherCallback} to receive the result of the Google Pay
     *                 payment flow
     */
    public GooglePayLauncher(@NonNull Fragment fragment,
                             @NonNull GooglePayLauncherCallback callback) {
        this(fragment.getActivity().getActivityResultRegistry(), fragment.getViewLifecycleOwner(),
                callback);
    }

    /**
     * Used to launch the Google Pay payment sheet from within an Android Activity. This class must be
     * instantiated before the Activity is created.
     *
     * @param activity the Android Activity from which you will launch the Google Pay payment sheet
     * @param callback a {@link GooglePayLauncherCallback} to receive the result of the Google Pay
     *                 payment flow
     */
    public GooglePayLauncher(@NonNull ComponentActivity activity,
                             @NonNull GooglePayLauncherCallback callback) {
        this(activity.getActivityResultRegistry(), activity, callback);
    }

    @VisibleForTesting
    GooglePayLauncher(ActivityResultRegistry registry, LifecycleOwner lifecycleOwner,
                      GooglePayLauncherCallback callback) {
        activityLauncher = registry.register(GOOGLE_PAY_RESULT, lifecycleOwner,
                new GooglePayActivityResultContract(), callback::onGooglePayLauncherResult);
    }

    /**
     * Launches the Google Pay payment sheet. This method cannot be called until the lifecycle of
     * the Fragment or Activity used to instantiate your {@link GooglePayLauncher} has reached the
     * CREATED state.
     *
     * @param googlePayPaymentAuthRequestParams the {@link GooglePayPaymentAuthRequestParams}
     *                                          received from invoking
     *                                          {@link GooglePayClient#createPaymentAuthRequest(GooglePayRequest, GooglePayPaymentAuthRequestCallback)}
     */
    public void launch(GooglePayPaymentAuthRequestParams googlePayPaymentAuthRequestParams) {
        activityLauncher.launch(googlePayPaymentAuthRequestParams);
    }
}
