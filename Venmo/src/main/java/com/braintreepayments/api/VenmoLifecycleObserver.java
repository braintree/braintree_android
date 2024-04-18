package com.braintreepayments.api;

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static com.braintreepayments.api.BraintreeRequestCodes.VENMO;

import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
class VenmoLifecycleObserver implements LifecycleEventObserver {

    private static final String VENMO_SECURE_RESULT = "com.braintreepayments.api.Venmo.RESULT";

    @VisibleForTesting
    VenmoClient venmoClient;

    @VisibleForTesting
    ActivityResultRegistry activityResultRegistry;

    @VisibleForTesting
    ActivityResultLauncher<VenmoIntentData> activityLauncher;

    @VisibleForTesting
    VenmoActivityResultContract venmoActivityResultContract = new VenmoActivityResultContract();

    VenmoLifecycleObserver(ActivityResultRegistry activityResultRegistry, VenmoClient venmoClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.venmoClient = venmoClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_CREATE) {
            activityLauncher = activityResultRegistry.register(VENMO_SECURE_RESULT, lifecycleOwner, new VenmoActivityResultContract(), new ActivityResultCallback<VenmoResult>() {
                @Override
                public void onActivityResult(VenmoResult venmoResult) {
                    venmoClient.onVenmoResult(venmoResult);
                }
            });
        }

        if (event == ON_RESUME) {
            FragmentActivity activity = null;
            if (lifecycleOwner instanceof FragmentActivity) {
                activity = (FragmentActivity) lifecycleOwner;
            } else if (lifecycleOwner instanceof Fragment) {
                activity = ((Fragment) lifecycleOwner).getActivity();
            }

            if (activity != null) {
                final FragmentActivity finalActivity = activity;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        BrowserSwitchResult resultToDeliver = null;

                        BrowserSwitchResult pendingResult = venmoClient.getBrowserSwitchResult(finalActivity);
                        if (pendingResult != null && pendingResult.getRequestCode() == VENMO) {
                            resultToDeliver = venmoClient.deliverBrowserSwitchResult(finalActivity);
                        }

                        BrowserSwitchResult pendingResultFromCache =
                                venmoClient.getBrowserSwitchResultFromNewTask(finalActivity);
                        if (pendingResultFromCache != null && pendingResultFromCache.getRequestCode() == VENMO) {
                            resultToDeliver =
                                    venmoClient.deliverBrowserSwitchResultFromNewTask(finalActivity);
                        }

                        if (resultToDeliver != null) {
                            venmoClient.onBrowserSwitchResult(resultToDeliver);
                        }
                    }
                });
            }
        }
    }

    void launch(VenmoIntentData venmoIntentData) {
        activityLauncher.launch(venmoIntentData);
    }
}
