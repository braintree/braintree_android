package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link VenmoClient#tokenizeVenmoAccount(FragmentActivity, VenmoRequest, VenmoTokenizeAccountCallback)}.
 */
public interface VenmoTokenizeAccountCallback {

    /**
     * @param error an exception that occurred while authorizing a Venmo account
     */
    void onResult(@Nullable Exception error);
}
