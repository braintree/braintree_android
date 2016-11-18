package com.braintreepayments.api.interfaces;

import com.visa.checkout.VisaMcomLibrary;

public interface VisaCheckoutListener extends BraintreeListener {
    void onVisaCheckoutLibraryCreated(VisaMcomLibrary visaMcomLibrary);
}
