package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Responsible for launching PayPal user authentication in a web browser
 */
public class PayPalLauncher {

    private final BrowserSwitchClient browserSwitchClient;

    /**
     * Used to launch the PayPal flow in a web browser and deliver results to your Activity
     */
    public PayPalLauncher() {
        this(new BrowserSwitchClient());
    }

    @VisibleForTesting
    PayPalLauncher(@NonNull BrowserSwitchClient browserSwitchClient) {
        this.browserSwitchClient = browserSwitchClient;
    }

    /**
     * Launches the PayPal flow by switching to a web browser for user authentication
     *
     * @param activity the Android Activity from which you will launch the web browser
     * @param paymentAuthRequest a {@link PayPalPaymentAuthRequest.ReadyToLaunch} received from
     *                           calling {@link PayPalClient#createPaymentAuthRequest(Context, PayPalRequest, PayPalPaymentAuthCallback)}
     * @return {@link PayPalPendingRequest} a {@link PayPalPendingRequest.Started} should be stored
     * to complete the flow upon return to app in
     * {@link PayPalLauncher#handleReturnToAppFromBrowser(PayPalPendingRequest.Started, Intent)},
     * or a {@link PayPalPendingRequest.Failure} with an error if the PayPal flow was unable to be
     * launched in a browser.
     */
    public PayPalPendingRequest launch(@NonNull ComponentActivity activity,
                       @NonNull PayPalPaymentAuthRequest.ReadyToLaunch paymentAuthRequest) {
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest.getRequestParams());
        } catch (BrowserSwitchException browserSwitchException) {
            Exception manifestInvalidError =
                    createBrowserSwitchError(browserSwitchException);
            return new PayPalPendingRequest.Failure(manifestInvalidError);
        }
        try {
            BrowserSwitchRequest request = browserSwitchClient.start(activity, paymentAuthRequest.getRequestParams().getBrowserSwitchOptions());
            return new PayPalPendingRequest.Started(new PayPalBrowserSwitchRequest(request));
        } catch (BrowserSwitchException e) {
            return new PayPalPendingRequest.Failure(e);
        }
    }

    /**
     * Captures and delivers the result of a the browser-based PayPal authentication flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke
     * {@link PayPalLauncher#launch(ComponentActivity, PayPalPaymentAuthRequest.ReadyToLaunch)}.
     * <p>
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     * <p>
     * This method will deliver a {@link PayPalPaymentAuthResult} to the
     * {@link PayPalLauncherCallback} used to instantiate this class. The
     * {@link PayPalPaymentAuthResult} should be passed to {@link PayPalClient#tokenize(PayPalPaymentAuthResult, PayPalTokenizeCallback)}
     *
     * @param pendingRequest the {@link PayPalPendingRequest.Started} stored after successfully
     *                       invoking {@link PayPalLauncher#launch(ComponentActivity, PayPalPaymentAuthRequest.ReadyToLaunch)}
     * @param intent         the intent to return to your application containing a deep link result
     *                       from the PayPal browser flow
     * @return a {@link PayPalPaymentAuthResult} that should be passed to
     * {@link PayPalClient#tokenize(PayPalPaymentAuthResult, PayPalTokenizeCallback)} to complete
     * the PayPal payment flow. Returns null if the user closed the browser to cancel the payment
     * flow, or returned to the app without completing the PayPal authentication flow.
     */
    @Nullable
    public PayPalPaymentAuthResult handleReturnToAppFromBrowser(
            @NonNull PayPalPendingRequest.Started pendingRequest, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(pendingRequest.getRequest().getBrowserSwitchRequest(), intent);
        PayPalPaymentAuthResult paymentAuthResult = null;
        if (result != null) {
           paymentAuthResult = new PayPalPaymentAuthResult(result);
        }
        return paymentAuthResult;
    }

    private void assertCanPerformBrowserSwitch(ComponentActivity activity, PayPalPaymentAuthRequestParams params)
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
