package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface PayPalBrowserSwitchResultCallback {
    // TODO: Change parameter to PayPalAccountNonce when API is finalized
    void onResult(PaymentMethodNonce nonce, Exception error);
}
