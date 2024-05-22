package com.braintreepayments.api.venmo;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BrowserSwitchResultInfo;

/**
 * Details of a {@link VenmoPaymentAuthResult.Success}
 */
public class VenmoPaymentAuthResultInfo {

    private BrowserSwitchResultInfo browserSwitchResultInfo;

    VenmoPaymentAuthResultInfo(@NonNull BrowserSwitchResultInfo browserSwitchResultInfo) {
        this.browserSwitchResultInfo = browserSwitchResultInfo;
    }

    BrowserSwitchResultInfo getBrowserSwitchResultInfo() {
        return browserSwitchResultInfo;
    }
}
