package com.braintreepayments.api.models;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.GooglePayment;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;

/**
 * Optional parameters to use when checking whether Google Payment API is supported and set up on the device.
 */
public class ReadyForGooglePayRequest {

    private boolean mExistingPaymentMethodRequired;

    /**
     * If set to true, then the {@link GooglePayment#isReadyToPay(BraintreeFragment, ReadyForGooglePayRequest, BraintreeResponseListener)}
     * method will call the listener with true if the customer is ready to pay with one or more of your
     * allowed card networks.
     *
     * @param value Indicates whether the customer must have one or more payment methods from your allowed card networks in order
     *              to be considered ready to pay with Google Pay
     *
     * @return {@link ReadyForGooglePayRequest}
     */
    public ReadyForGooglePayRequest existingPaymentMethodRequired(boolean value) {
        mExistingPaymentMethodRequired = value;
        return this;
    }

    public boolean isExistingPaymentMethodRequired() {
        return mExistingPaymentMethodRequired;
    }
}
