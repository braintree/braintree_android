package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
