package com.braintreepayments.api;

import android.content.Intent;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

/**
 * Responsible for launching a SEPA mandate in a web browser
 */
public class SEPADirectDebitLauncher {

    private final BrowserSwitchClient browserSwitchClient;

    /**
     * Used to launch the SEPA mandate in a web browser and deliver results to your Activity
     */
    public SEPADirectDebitLauncher() {
        this(new BrowserSwitchClient());
    }

    @VisibleForTesting
    SEPADirectDebitLauncher(@NonNull BrowserSwitchClient browserSwitchClient) {
        this.browserSwitchClient = browserSwitchClient;
    }

    /**
     * Launches the SEPA mandate by switching to a web browser for user authentication
     *
     * @param activity       an Android {@link FragmentActivity}
     * @param paymentAuthRequest the result of the SEPA mandate received from invoking
     *                       {@link SEPADirectDebitClient#createPaymentAuthRequest(SEPADirectDebitRequest, SEPADirectDebitPaymentAuthRequestCallback)}
     * @return {@link SEPADirectDebitPendingRequest} a {@link SEPADirectDebitPendingRequest.Started}
     * should be stored to complete the flow upon return to app in
     * {@link SEPADirectDebitLauncher#handleReturnToAppFromBrowser(SEPADirectDebitPendingRequest.Started, Intent)},
     * or a {@link SEPADirectDebitPendingRequest.Failure} with an error if the SEPA flow was unable
     * to be launched in a browser.
     */
    @NonNull
    public SEPADirectDebitPendingRequest launch(@NonNull ComponentActivity activity, @NonNull
    SEPADirectDebitPaymentAuthRequest.ReadyToLaunch paymentAuthRequest) {
        SEPADirectDebitPaymentAuthRequestParams params = paymentAuthRequest.getRequestParams();
        BrowserSwitchPendingRequest browserSwitchPendingRequest = browserSwitchClient.start(activity, params.getBrowserSwitchOptions());
        if (browserSwitchPendingRequest instanceof BrowserSwitchPendingRequest.Started) {
            return new SEPADirectDebitPendingRequest.Started(new SEPADirectDebitBrowserSwitchRequest(((BrowserSwitchPendingRequest.Started) browserSwitchPendingRequest)));
        } else if (browserSwitchPendingRequest instanceof BrowserSwitchPendingRequest.Failure) {
            return new SEPADirectDebitPendingRequest.Failure(((BrowserSwitchPendingRequest.Failure) browserSwitchPendingRequest).getCause());
        }
        return new SEPADirectDebitPendingRequest.Failure(new BraintreeException("An unexpected error occurred"));
    }

    /**
     * Captures and delivers the result of the browser-based SEPA mandate flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke {@link SEPADirectDebitLauncher#launch(ComponentActivity, SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)}.
     * <p>
     * If the Activity used to launch the SEPA mandate is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity.
     *
     * @param pendingRequest the {@link SEPADirectDebitPendingRequest.Started} stored after successfully
     *                       invoking {@link SEPADirectDebitLauncher#launch(ComponentActivity, SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)}
     * @param intent  the intent to return to your application containing a deep link result from
     *                the SEPA mandate flow
     * @return a {@link SEPADirectDebitPaymentAuthResult} that should be passed to
     * {@link SEPADirectDebitClient#tokenize(SEPADirectDebitPaymentAuthResult, SEPADirectDebitTokenizeCallback)}
     * to complete the flow.
     */
    @Nullable
    public SEPADirectDebitPaymentAuthResult handleReturnToAppFromBrowser(@NonNull SEPADirectDebitPendingRequest.Started pendingRequest, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(pendingRequest.getRequest()
                .getBrowserSwitchPendingRequest(), intent);
        if (result != null) {
            return new SEPADirectDebitPaymentAuthResult(result);
        }
        return null;
    }
}
