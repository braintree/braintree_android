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
     * {@link PayPalLauncher#launch(FragmentActivity, PayPalPaymentAuthRequestParams)}
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
     * @param paymentAuthRequest a request to launch the PayPal web flow created in
     *                       {@link PayPalClient#createPaymentAuthRequest(FragmentActivity,
     *                       PayPalRequest, PayPalPaymentAuthCallback)}
     */
    public void launch(@NonNull FragmentActivity activity,
                       @NonNull PayPalPaymentAuthRequestParams paymentAuthRequest) {
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest);
        } catch (BrowserSwitchException browserSwitchException) {
            Exception manifestInvalidError =
                    createBrowserSwitchError(browserSwitchException);
            callback.onResult(new PayPalPaymentAuthResult(manifestInvalidError));
            return;
        }
        try {
            browserSwitchClient.start(activity, paymentAuthRequest.getBrowserSwitchOptions());
        } catch (BrowserSwitchException e) {
            callback.onResult(new PayPalPaymentAuthResult(e));
        }
    }

    /**
     * Captures and delivers the result of a the browser-based PayPal authentication flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke {@link PayPalLauncher#launch(FragmentActivity, PayPalPaymentAuthRequestParams)}.
     * <p>
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity, after invoking setIntent(intent).
     * <p>
     * This method will deliver a {@link PayPalPaymentAuthResult} to the
     * {@link PayPalLauncherCallback} used to instantiate this class. The
     * {@link PayPalPaymentAuthResult} should be passed to
     * {@link PayPalClient#tokenize(PayPalPaymentAuthResult, PayPalTokenizeCallback)}
     *
     * @param context the context used to check for pending results
     * @param intent  the intent to return to your application containing a deep link result from
     *                the PayPal browser flow
     */
    public void handleReturnToAppFromBrowser(@NonNull Context context, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(context, PAYPAL, intent);
        if (result != null) {
           callback.onResult(new PayPalPaymentAuthResult(result));
           browserSwitchClient.clearActiveRequests(context);
        }
    }

    private void assertCanPerformBrowserSwitch(FragmentActivity activity, PayPalPaymentAuthRequestParams params)
            throws BrowserSwitchException {
        browserSwitchClient.assertCanPerformBrowserSwitch(activity, params.getBrowserSwitchOptions());
    }

    private static Exception createBrowserSwitchError(BrowserSwitchException exception) {
        return new BraintreeException(
                "AndroidManifest.xml is incorrectly configured or another app " +
                        "defines the same browser switch url as this app. See " +
                        "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/android/v4#browser-switch-setup " +
                        "for the correct configuration: " + exception.getMessage());
    }
}
