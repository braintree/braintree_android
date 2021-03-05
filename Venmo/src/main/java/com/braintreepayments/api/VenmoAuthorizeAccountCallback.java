package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link VenmoClient#authorizeAccount(FragmentActivity, boolean, String, VenmoAuthorizeAccountCallback)}.
 */
public interface VenmoAuthorizeAccountCallback {

    /**
     * @param error an exception that occurred while authorizing a Venmo account
     */
    void onResult(@Nullable Exception error);
}
