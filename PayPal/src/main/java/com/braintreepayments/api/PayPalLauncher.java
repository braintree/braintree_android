package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

/**
 * Responsible for launching PayPal user authentication in a web browser
 */
public class PayPalLauncher {

    private final BrowserSwitchClient browserSwitchClient;
    private final PayPalLauncherCallback callback;

    /**
     * Used to launch the PayPal flow in a web browser and deliver results to your Activity
     *
     * @param callback a {@link PayPalLauncherCallback} to handle the result of
     * {@link PayPalLauncher#launch(FragmentActivity, PayPalResponse)}
     */
    public PayPalLauncher(@NonNull PayPalLauncherCallback callback) {
        this(new BrowserSwitchClient(), callback);
    }

    @VisibleForTesting
    PayPalLauncher(@NonNull BrowserSwitchClient browserSwitchClient,
                   PayPalLauncherCallback callback) {
        this.browserSwitchClient = browserSwitchClient;
        this.callback = callback;
    }

    /**
     * Launches the PayPal flow by switching to a web browser for user authentication and
     * delivers results to the {@link PayPalLauncherCallback} passed into
     * {@link PayPalLauncher#PayPalLauncher(PayPalLauncherCallback)}
     *
     * @param activity       an Android {@link FragmentActivity}
     * @param payPalResponse the result of the PayPal web authentication flow received from invoking
     *                       {@link PayPalClient#createPaymentAuthRequest(FragmentActivity,
     *                       PayPalRequest, PayPalFlowStartedCallback)}
     */
    public void launch(@NonNull FragmentActivity activity, @NonNull PayPalResponse payPalResponse) {
        try {
            browserSwitchClient.start(activity, payPalResponse.getBrowserSwitchOptions());
        } catch (BrowserSwitchException e) {
            callback.onResult(new PayPalBrowserSwitchResult(e));
        }
    }

    /**
     * Captures and delivers the result of a the browser-based PayPal authentication flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke {@link PayPalLauncher#launch(FragmentActivity, PayPalResponse)}.
     * <p>
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity, after invoking setIntent(intent).
     * <p>
     * This method will deliver a {@link PayPalBrowserSwitchResult} to the
     * {@link PayPalLauncherCallback} used to instantiate this class. The
     * {@link PayPalBrowserSwitchResult} should be passed to
     * {@link PayPalClient#tokenize(PayPalBrowserSwitchResult, PayPalBrowserSwitchResultCallback)}
     *
     * @param context the context used to check for pending results
     * @param intent  the intent to return to your application containing a deep link result from
     *                the PayPal browser flow
     */
    public void handleReturnToAppFromBrowser(@NonNull Context context, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(context, PAYPAL, intent);
        if (result != null) {
           callback.onResult(new PayPalBrowserSwitchResult(result));
           browserSwitchClient.clearActiveRequests(context);
        }
    }
}
