package com.braintreepayments.api;

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

//    public void launch(@NonNull FragmentActivity activity, @NonNull PayPalResponse payPalResponse) {
//        try {
//            browserSwitchClient.start(activity, payPalResponse.getBrowserSwitchOptions());
//        } catch (BrowserSwitchException e) {
//            callback.onResult(new SEPADirectDebitBrowserSwitchResult(e));
//        }
//    }
}
