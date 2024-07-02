package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Deprecated. Use PayPal module instead.
 * @hide
 */
@Deprecated
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PayPalNativeCheckoutInternalClientCallback {
    void onResult(@Nullable PayPalNativeCheckoutResponse payPalResponse, @Nullable Exception error);
}
