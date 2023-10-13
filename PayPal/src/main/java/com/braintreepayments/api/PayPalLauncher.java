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
     * @param activity       an Android {@link FragmentActivity}
     * @param payPalResponse the result of the PayPal web authentication flow received from invoking
     *                       {@link PayPalClient#tokenizePayPalAccount(FragmentActivity,
     *                       PayPalRequest, PayPalFlowStartedCallback)}
     * @throws BrowserSwitchException if there is an error launching the PayPal web flow
     */
    public void launch(@NonNull FragmentActivity activity, @NonNull PayPalResponse payPalResponse)
            throws BrowserSwitchException {
        browserSwitchClient.start(activity, payPalResponse.getBrowserSwitchOptions());
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
     *
     * @param context the context used to check for pending results
     * @param intent  the intent to return to your application containing a deep link result from
     *                the PayPal browser flow
     * @return a {@link BrowserSwitchResult} to be passed to
     * {@link PayPalClient#tokenizePayPalAccount(FragmentActivity, PayPalRequest,
     * PayPalFlowStartedCallback)}
     */
    public BrowserSwitchResult deliverResult(@NonNull Context context, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(context, PAYPAL, intent);
        browserSwitchClient.clearActiveRequests(context);
        return result;
    }
}
