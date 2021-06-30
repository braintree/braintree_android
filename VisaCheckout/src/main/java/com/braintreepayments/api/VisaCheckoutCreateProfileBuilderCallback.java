package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.visa.checkout.Profile;

/**
 * Callback for receiving result of
 * {@link VisaCheckoutClient#createProfileBuilder(VisaCheckoutCreateProfileBuilderCallback)}.
 */
public interface VisaCheckoutCreateProfileBuilderCallback {

    /**
     * @param profileBuilder Visa profile builder
     * @param error an exception that occurred while creating a Visa profile
     */
    void onResult(@Nullable Profile.ProfileBuilder profileBuilder, @Nullable Exception error);
}
