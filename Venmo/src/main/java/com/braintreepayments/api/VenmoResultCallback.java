package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link VenmoClient#onActivityResult(Context, int, Intent, VenmoResultCallback)}.
 */
public interface VenmoResultCallback {

    /**
     * @param venmoAccountNonce {@link VenmoAccountNonce}
     * @param error an exception that occurred while processing a Venmo result
     */
    void onResult(@Nullable VenmoAccountNonce venmoAccountNonce, @Nullable Exception error);
}
