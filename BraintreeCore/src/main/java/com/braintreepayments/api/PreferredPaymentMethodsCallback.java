package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

/**
 * Callback for receiving result of {@link PreferredPaymentMethodsClient#fetchPreferredPaymentMethods(Context, PreferredPaymentMethodsCallback)}.
 * This interface is currently in beta and may change in future releases.
 */
public interface PreferredPaymentMethodsCallback {

    /**
     * @param preferredPaymentMethodsResult {@link PreferredPaymentMethodsResult}
     */
    void onResult(@NonNull PreferredPaymentMethodsResult preferredPaymentMethodsResult);
}
