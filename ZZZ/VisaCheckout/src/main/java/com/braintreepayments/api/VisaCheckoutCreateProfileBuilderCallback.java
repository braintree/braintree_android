package com.braintreepayments.api;

import com.visa.checkout.Profile;

public interface VisaCheckoutCreateProfileBuilderCallback {
    void onResult(Profile.ProfileBuilder profileBuilder, Exception error);
}
