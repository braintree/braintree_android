package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VenmoPaymentAuthResultInfo {

    private Exception error;
    private String paymentContextId;
    private String venmoAccountNonce;
    private String venmoUsername;
    private BrowserSwitchResultInfo browserSwitchResultInfo;

    VenmoPaymentAuthResultInfo(@Nullable String paymentContextId, @Nullable String venmoAccountNonce,
                               @Nullable String venmoUsername, @Nullable Exception error) {
        this.paymentContextId = paymentContextId;
        this.venmoAccountNonce = venmoAccountNonce;
        this.venmoUsername = venmoUsername;
        this.error = error;
    }

    VenmoPaymentAuthResultInfo(@NonNull BrowserSwitchResultInfo browserSwitchResultInfo) {
        this.browserSwitchResultInfo = browserSwitchResultInfo;
    }

    Exception getError() {
        return error;
    }

    String getPaymentContextId() {
        return paymentContextId;
    }

    String getVenmoAccountNonce() {
        return venmoAccountNonce;
    }

    String getVenmoUsername() {
        return venmoUsername;
    }

    BrowserSwitchResultInfo getBrowserSwitchResultInfo() {
        return browserSwitchResultInfo;
    }
}
