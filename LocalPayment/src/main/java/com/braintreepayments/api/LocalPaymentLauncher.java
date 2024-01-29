package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Responsible for launching local payment user authentication in a web browser
 */
public class LocalPaymentLauncher {

    private final BrowserSwitchClient browserSwitchClient;

    /**
     * Used to launch the local payment flow in a web browser and deliver results to your Activity
     */
    public LocalPaymentLauncher() {
        this(new BrowserSwitchClient());
    }

    @VisibleForTesting
    LocalPaymentLauncher(@NonNull BrowserSwitchClient browserSwitchClient) {
        this.browserSwitchClient = browserSwitchClient;
    }

    /**
     * Launches the local payment flow by switching to a web browser for user authentication.
     *
     * @param activity           an Android {@link ComponentActivity}
     * @param localPaymentAuthRequest the payment auth request created in
     *                           {@link LocalPaymentClient#createPaymentAuthRequest(LocalPaymentRequest, LocalPaymentAuthCallback)}
     * @return {@link LocalPaymentPendingRequest} a {@link LocalPaymentPendingRequest.Started} should
     * be stored to complete the flow upon return to app in
     * {@link LocalPaymentLauncher#handleReturnToAppFromBrowser(LocalPaymentPendingRequest.Started, Intent)},
     * or a {@link LocalPaymentPendingRequest.Failure} with an error if the local payment flow was
     * unable to be launched in a browser.
     */
    public LocalPaymentPendingRequest launch(@NonNull ComponentActivity activity,
                       @NonNull LocalPaymentAuthRequest.ReadyToLaunch localPaymentAuthRequest) {
        LocalPaymentAuthRequestParams params = localPaymentAuthRequest.getRequestParams();
        BrowserSwitchPendingRequest browserSwitchPendingRequest = browserSwitchClient.start(activity, params.getBrowserSwitchOptions());
        if (browserSwitchPendingRequest instanceof BrowserSwitchPendingRequest.Started) {
            return new LocalPaymentPendingRequest.Started(new LocalPaymentBrowserSwitchRequest(((BrowserSwitchPendingRequest.Started) browserSwitchPendingRequest)));
        } else if (browserSwitchPendingRequest instanceof BrowserSwitchPendingRequest.Failure) {
            return new LocalPaymentPendingRequest.Failure(((BrowserSwitchPendingRequest.Failure) browserSwitchPendingRequest).getCause());
        }
        return new LocalPaymentPendingRequest.Failure(new BraintreeException("An unexpected error occurred"));
    }

    /**
     * Captures and delivers the result of a the browser-based local payment authentication flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke
     * {@link LocalPaymentLauncher#launch(ComponentActivity, LocalPaymentAuthRequest.ReadyToLaunch)}.
     * <p>
     * If the Activity used to launch the PayPal flow has is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     *
     * @param pendingRequest the {@link LocalPaymentPendingRequest.Started} stored after successfully
     *                       invoking {@link LocalPaymentLauncher#launch(ComponentActivity, LocalPaymentAuthRequest.ReadyToLaunch)}
     * @param intent         the intent to return to your application containing a deep link result
     *                       from the local payment browser flow
     * @return a {@link LocalPaymentAuthResult} that should be passed to
     * {@link LocalPaymentClient#tokenize(Context, LocalPaymentAuthResult, LocalPaymentTokenizeCallback)}
     * to complete the flow
     */
    @Nullable
    public LocalPaymentAuthResult handleReturnToAppFromBrowser(LocalPaymentPendingRequest.Started pendingRequest,
                                             @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(pendingRequest.getRequest()
                        .getBrowserSwitchPendingRequest(), intent);
        if (result != null) {
            return new LocalPaymentAuthResult(result);
        }
        return null;
    }
}
