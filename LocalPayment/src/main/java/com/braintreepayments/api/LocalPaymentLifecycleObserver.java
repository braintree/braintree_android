package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.LOCAL_PAYMENT;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

// NEXT_MAJOR_VERSION: Update to implement DefaultLifeCycleObserver when Java 7 support is explicitly dropped.
class LocalPaymentLifecycleObserver implements LifecycleEventObserver {

    @VisibleForTesting
    LocalPaymentClient localPaymentClient;

    LocalPaymentLifecycleObserver(@NonNull LocalPaymentClient localPaymentClient) {
        this.localPaymentClient = localPaymentClient;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
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

                        BrowserSwitchResult pendingResult = localPaymentClient.getBrowserSwitchResult(finalActivity);
                        if (pendingResult != null && pendingResult.getRequestCode() == LOCAL_PAYMENT) {
                            resultToDeliver = localPaymentClient.deliverBrowserSwitchResult(finalActivity);
                        }

                        BrowserSwitchResult pendingResultFromCache =
                                localPaymentClient.getBrowserSwitchResultFromCache(finalActivity);
                        if (pendingResultFromCache != null && pendingResultFromCache.getRequestCode() == LOCAL_PAYMENT) {
                            resultToDeliver =
                                    localPaymentClient.deliverBrowserSwitchResultFromCache(finalActivity);
                        }

                        if (resultToDeliver != null) {
                            localPaymentClient.onBrowserSwitchResult(finalActivity, resultToDeliver);
                        }
                    }
                });
            }
        }
    }
}
