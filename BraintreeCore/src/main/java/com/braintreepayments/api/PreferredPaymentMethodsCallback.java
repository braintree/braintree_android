package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link PreferredPaymentMethodsClient#fetchPreferredPaymentMethods(Context, PreferredPaymentMethodsCallback)}.
 * This interface is currently in beta and may be removed in future releases.
 * @hide
 */
public interface PreferredPaymentMethodsCallback {

    /**
     * @param preferredPaymentMethodsResult {@link PreferredPaymentMethodsResult}
     */
    void onResult(@NonNull PreferredPaymentMethodsResult preferredPaymentMethodsResult);
}
