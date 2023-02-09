package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.THREE_D_SECURE;

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
class ThreeDSecureLifecycleObserver implements LifecycleEventObserver {

    private static final String THREE_D_SECURE_RESULT = "com.braintreepayments.api.ThreeDSecure.RESULT";

    @VisibleForTesting
    ThreeDSecureClient threeDSecureClient;

    @VisibleForTesting
    ActivityResultRegistry activityResultRegistry;

    @VisibleForTesting
    ActivityResultLauncher activityLauncher;

    ThreeDSecureLifecycleObserver(ActivityResultRegistry activityResultRegistry, ThreeDSecureClient threeDSecureClient) {
        this.activityResultRegistry = activityResultRegistry;
        this.threeDSecureClient = threeDSecureClient;
    }

    void launch(ThreeDSecureResult threeDSecureResult) {
        activityLauncher.launch(threeDSecureResult);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                activityLauncher = activityResultRegistry.register(THREE_D_SECURE_RESULT, lifecycleOwner, new ThreeDSecureActivityResultContract(), new ActivityResultCallback<CardinalResult>() {
                    @Override
                    public void onActivityResult(CardinalResult cardinalResult) {
                        threeDSecureClient.onCardinalResult(cardinalResult);
                    }
                });
            case ON_RESUME:
                FragmentActivity activity = null;
                if (lifecycleOwner instanceof FragmentActivity) {
                    activity = (FragmentActivity) lifecycleOwner;
                } else if (lifecycleOwner instanceof Fragment) {
                    activity = ((Fragment) lifecycleOwner).getActivity();
                }

                if (activity != null) {

                    /*
                     * WORKAROUND: Android 9 onResume() / onNewIntent() are called in an unpredictable way.
                     *
                     * We instruct merchants to call `setIntent(intent)` in onNewIntent so the SDK can
                     * process deep links to activities that are already running e.g. "singleTop" launch
                     * mode activities.
                     *
                     * On Android 9, onResume() can be called multiple times – once before and once after
                     * onNewIntent(). The SDK parses the deep link URI to determine if a browser-based
                     * payment flow is successful.
                     *
                     * In order to make sure the deep link intent is available to the SDK when the activity
                     * is RESUMED, we run browser switching logic on the next loop of the main thread.
                     * This prevents false negatives from occurring, where the SDK thinks the user has
                     * returned to the app without completing the flow, when in fact the deep link intent
                     * has not yet been delivered via onNewIntent.
                     */
                    final FragmentActivity finalActivity = activity;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            BrowserSwitchResult resultToDeliver = null;

                            BrowserSwitchResult pendingResult = threeDSecureClient.getBrowserSwitchResult(finalActivity);
                            if (pendingResult != null && pendingResult.getRequestCode() == THREE_D_SECURE) {
                                resultToDeliver = threeDSecureClient.deliverBrowserSwitchResult(finalActivity);
                            }

                            BrowserSwitchResult pendingResultFromCache =
                                    threeDSecureClient.getBrowserSwitchResultFromNewTask(finalActivity);
                            if (pendingResultFromCache != null && pendingResultFromCache.getRequestCode() == THREE_D_SECURE) {
                                resultToDeliver =
                                        threeDSecureClient.deliverBrowserSwitchResultFromNewTask(finalActivity);
                            }

                            if (resultToDeliver != null) {
                                threeDSecureClient.onBrowserSwitchResult(resultToDeliver);
                            }
                        }
                    });
                }
        }
    }
}
