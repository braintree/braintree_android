package com.braintreepayments.api.venmo;

import androidx.activity.ComponentActivity;

import com.braintreepayments.api.BrowserSwitchOptions;

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
