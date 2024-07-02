package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PayPalNativeCheckoutInternalClientCallback {
    void onResult(@Nullable PayPalNativeCheckoutResponse payPalResponse, @Nullable Exception error);
}
