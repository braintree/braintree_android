package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.SEPA_DEBIT;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

/**
 * Responsible for launching a SEPA mandate in a web browser
 */
public class SEPADirectDebitLauncher {

    private final BrowserSwitchClient browserSwitchClient;
    private final SEPADirectDebitLauncherCallback callback;

    /**
     * Used to launch the SEPA mandate in a web browser and deliver results to your Activity
     *
     * @param callback a {@link SEPADirectDebitLauncherCallback} to handle the result of
     * {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)}
     */
    public SEPADirectDebitLauncher(@NonNull SEPADirectDebitLauncherCallback callback) {
        this(new BrowserSwitchClient(), callback);
    }

    @VisibleForTesting
    SEPADirectDebitLauncher(@NonNull BrowserSwitchClient browserSwitchClient,
                            SEPADirectDebitLauncherCallback callback) {
        this.browserSwitchClient = browserSwitchClient;
        this.callback = callback;
    }

    /**
     * Launches the SEPA mandate by switching to a web browser for user authentication and
     * delivers results to the {@link SEPADirectDebitLauncherCallback} passed into
     * {@link SEPADirectDebitLauncher#SEPADirectDebitLauncher(SEPADirectDebitLauncherCallback)}
     *
     * @param activity       an Android {@link FragmentActivity}
     * @param paymentAuthRequest the result of the SEPA mandate received from invoking
     *                       {@link SEPADirectDebitClient#createPaymentAuthRequest(SEPADirectDebitRequest, SEPADirectDebitPaymentAuthRequestCallback)}
     */
    public void launch(@NonNull FragmentActivity activity, @NonNull
    SEPADirectDebitPaymentAuthRequest.ReadyToLaunch paymentAuthRequest) {
        try {
            SEPADirectDebitPaymentAuthRequestParams params = paymentAuthRequest.getRequestParams();
            browserSwitchClient.start(activity, params.getBrowserSwitchOptions());
        } catch (BrowserSwitchException e) {
            callback.onResult(new SEPADirectDebitPaymentAuthResult(e));
        }
    }

    /**
     * Captures and delivers the result of the browser-based SEPA mandate flow.
     * <p>
     * For most integrations, this method should be invoked in the onResume method of the Activity
     * used to invoke {@link SEPADirectDebitLauncher#launch(FragmentActivity, SEPADirectDebitPaymentAuthRequest.ReadyToLaunch)}.
     * <p>
     * If the Activity used to launch the SEPA mandate is configured with
     * android:launchMode="singleTop", this method should be invoked in the onNewIntent method of
     * the Activity, after invoking setIntent(intent).
     * <p>
     * This method will deliver a {@link SEPADirectDebitPaymentAuthResult} to the
     * {@link SEPADirectDebitLauncherCallback} used to instantiate this class. The
     * {@link SEPADirectDebitPaymentAuthResult} should be passed to
     * {@link SEPADirectDebitClient#tokenize(SEPADirectDebitPaymentAuthResult, SEPADirectDebitTokenizeCallback)} 
     *
     * @param context the context used to check for pending results
     * @param intent  the intent to return to your application containing a deep link result from
     *                the SEPA mandate flow
     */
    public void handleReturnToAppFromBrowser(@NonNull Context context, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(context, SEPA_DEBIT, intent);
        if (result != null) {
            callback.onResult(new SEPADirectDebitPaymentAuthResult(result));
            browserSwitchClient.clearActiveRequests(context);
        }
    }
}
