package com.braintreepayments.api.venmo;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BrowserSwitchFinalResult;

/**
 * Details of a {@link VenmoPaymentAuthResult.Success}
 */
public class VenmoPaymentAuthResultInfo {

    private BrowserSwitchFinalResult.Success browserSwitchSuccess;

    VenmoPaymentAuthResultInfo(@NonNull BrowserSwitchFinalResult.Success browserSwitchSuccess) {
        this.browserSwitchSuccess = browserSwitchSuccess;
    }

    BrowserSwitchFinalResult.Success getBrowserSwitchResultInfo() {
        return browserSwitchSuccess;
    }
}
