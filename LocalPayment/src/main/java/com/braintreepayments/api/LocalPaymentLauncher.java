package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.LOCAL_PAYMENT;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

/**
 * Responsible for launching local payment user authentication in a web browser
 */
public class LocalPaymentLauncher {

    private final BrowserSwitchClient browserSwitchClient;
    private final LocalPaymentLauncherCallback callback;

    /**
     * Used to launch the local payment flow in a web browser and deliver results to your Activity
     *
     * @param callback a {@link LocalPaymentLauncherCallback} to handle the result of
     *                 {@link LocalPaymentLauncher#launch(FragmentActivity, LocalPaymentAuthRequestParams)}
     */
    public LocalPaymentLauncher(@NonNull LocalPaymentLauncherCallback callback) {
        this(new BrowserSwitchClient(), callback);
    }

    @VisibleForTesting
    LocalPaymentLauncher(@NonNull BrowserSwitchClient browserSwitchClient,
                         LocalPaymentLauncherCallback callback) {
        this.browserSwitchClient = browserSwitchClient;
        this.callback = callback;
    }

    /**
     * Launches the local payment flow by switching to a web browser for user authentication and
     * delivers results to the {@link LocalPaymentLauncherCallback} passed into
     * {@link LocalPaymentLauncher#LocalPaymentLauncher(LocalPaymentLauncherCallback)}
     *
     * @param activity           an Android {@link FragmentActivity}
     * @param localPaymentAuthRequestParams the payment auth request created in
     *                           {@link LocalPaymentClient#createPaymentAuthRequest(LocalPaymentRequest,
     *                           LocalPaymentInternalAuthRequestCallback)}
     */
    public void launch(@NonNull FragmentActivity activity,
                       @NonNull LocalPaymentAuthRequestParams localPaymentAuthRequestParams) {
        try {
            browserSwitchClient.start(activity, localPaymentAuthRequestParams.getBrowserSwitchOptions());
        } catch (BrowserSwitchException e) {
            callback.onResult(new LocalPaymentAuthResult(e));
        }
    }

    /**
     * Captures and delivers the result of a the browser-based local payment authentication flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke {@link LocalPaymentLauncher#launch(FragmentActivity, LocalPaymentAuthRequestParams)}.
     * <p>
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity, after invoking setIntent(intent).
     * <p>
     * This method will deliver a {@link LocalPaymentAuthResult} to the
     * {@link LocalPaymentLauncherCallback} used to instantiate this class. The
     * {@link LocalPaymentAuthResult} should be passed to
     * {@link LocalPaymentClient#tokenize(Context, LocalPaymentAuthResult, LocalPaymentInternalTokenizeCallback)}
     *
     * @param context the context used to check for pending results
     * @param intent  the intent to return to your application containing a deep link result from
     *                the local payment browser flow
     */
    public void handleReturnToAppFromBrowser(@NonNull Context context, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(context, LOCAL_PAYMENT,
                intent);
        if (result != null) {
            callback.onResult(new LocalPaymentAuthResult(result));
            browserSwitchClient.clearActiveRequests(context);
        }
    }
}
