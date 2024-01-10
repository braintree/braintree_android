package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.PAYPAL;

import android.content.Context;
import android.content.Intent;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

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
     * Launches the PayPal flow by switching to a web browser for user authentication and
     * delivers results to the {@link PayPalLauncherCallback} passed into
     * {@link PayPalLauncher#PayPalLauncher()}
     *
     * @param activity       an Android {@link FragmentActivity}
     * @param paymentAuthRequest a request to launch the PayPal web flow created in
     *                       {@link PayPalClient#createPaymentAuthRequest(Context, PayPalRequest, PayPalPaymentAuthCallback)}
     */
    public PayPalPendingRequest launch(@NonNull ComponentActivity activity,
                       @NonNull PayPalPaymentAuthRequestParams paymentAuthRequest) {
        try {
            assertCanPerformBrowserSwitch(activity, paymentAuthRequest);
        } catch (BrowserSwitchException browserSwitchException) {
            Exception manifestInvalidError =
                    createBrowserSwitchError(browserSwitchException);
            return new PayPalPendingRequest.Failure(manifestInvalidError);
        }
        try {
            BrowserSwitchRequest request = browserSwitchClient.start(activity, paymentAuthRequest.getBrowserSwitchOptions());
            return new PayPalPendingRequest.Success(new PayPalBrowserSwitchRequest(request));
        } catch (BrowserSwitchException e) {
            return new PayPalPendingRequest.Failure(e);
        }
    }

    /**
     * Captures and delivers the result of a the browser-based PayPal authentication flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke
     * {@link PayPalLauncher#launch(FragmentActivity, PayPalPaymentAuthRequestParams)}.
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
     * @param pendingRequest the context used to check for pending results
     * @param intent         the intent to return to your application containing a deep link result
     *                       from the PayPal browser flow
     */
    public PayPalPaymentAuthResult handleReturnToAppFromBrowser(
            @NonNull PayPalPendingRequest.Success pendingRequest, @NonNull Intent intent) {
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
