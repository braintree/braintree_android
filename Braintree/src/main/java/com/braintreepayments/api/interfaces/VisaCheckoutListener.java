package com.braintreepayments.api.interfaces;

import com.visa.checkout.VisaMcomLibrary;

public interface VisaCheckoutListener {
    void onVisaCheckoutLibraryCreated(VisaMcomLibrary visaMcomLibrary);
}
