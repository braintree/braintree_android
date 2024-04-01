package com.braintreepayments.api;

import androidx.activity.ComponentActivity;

/**
 * Used to request Venmo authentication via {@link VenmoLauncher#launch(ComponentActivity, VenmoPaymentAuthRequest.ReadyToLaunch)} )}
 */
public class VenmoPaymentAuthRequestParams {

    private BrowserSwitchOptions browserSwitchOptions;

    VenmoPaymentAuthRequestParams(BrowserSwitchOptions browserSwitchOptions) {
        this.browserSwitchOptions = browserSwitchOptions;
    }

    BrowserSwitchOptions getBrowserSwitchOptions() {
        return browserSwitchOptions;
    }
}
