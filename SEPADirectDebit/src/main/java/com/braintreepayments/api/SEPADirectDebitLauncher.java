package com.braintreepayments.api;

import static com.braintreepayments.api.BraintreeRequestCodes.SEPA_DEBIT;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

public class SEPADirectDebitLauncher {

    private final BrowserSwitchClient browserSwitchClient;
    private final SEPADirectDebitLauncherCallback callback;

    public SEPADirectDebitLauncher(@NonNull SEPADirectDebitLauncherCallback callback) {
        this(new BrowserSwitchClient(), callback);
    }

    @VisibleForTesting
    SEPADirectDebitLauncher(@NonNull BrowserSwitchClient browserSwitchClient,
                            SEPADirectDebitLauncherCallback callback) {
        this.browserSwitchClient = browserSwitchClient;
        this.callback = callback;
    }

    // TODO: - Launch SEPA flow in web browser & deliver results to SEPADirectDebitLauncherCallback passed into SEPADirectDebitLauncher init
    public void launch(@NonNull FragmentActivity activity, @NonNull SEPADirectDebitResponse sepaDirectDebitResponse) {
        try {
            browserSwitchClient.start(activity, sepaDirectDebitResponse.getBrowserSwitchOptions());
        } catch (BrowserSwitchException e) {
            callback.onResult(new SEPADirectDebitBrowserSwitchResult(e));
        }
    }

    // TODO: - Capture & deliver result of browser-based SEPA flow. Invoke in onResume.
    public void handleReturnToAppFromBrowser(@NonNull Context context, @NonNull Intent intent) {
        BrowserSwitchResult result = browserSwitchClient.parseResult(context, SEPA_DEBIT, intent);
        if (result != null) {
            callback.onResult(new SEPADirectDebitBrowserSwitchResult(result));
            browserSwitchClient.clearActiveRequests(context);
        }
    }
}
