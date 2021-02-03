package com.braintreepayments.api;

/**
 * Optional parameters to use when checking whether Google Pay is supported and set up on the customer's device.
 */
public class ReadyForGooglePaymentRequest {

    private boolean mExistingPaymentMethodRequired;

    /**
     * If set to true, then the {@link GooglePayment#isReadyToPay(BraintreeFragment, ReadyForGooglePaymentRequest, BraintreeResponseListener)}
     * method will call the listener with true if the customer is ready to pay with one or more of your
     * supported card networks.
     *
     * @param existingPaymentMethodRequired Indicates whether the customer must already have at least one payment method from your supported
     *              card networks in order to be considered ready to pay with Google Pay
     *
     * @return {@link ReadyForGooglePaymentRequest}
     */
    public ReadyForGooglePaymentRequest existingPaymentMethodRequired(boolean existingPaymentMethodRequired) {
        mExistingPaymentMethodRequired = existingPaymentMethodRequired;
        return this;
    }

    public boolean isExistingPaymentMethodRequired() {
        return mExistingPaymentMethodRequired;
    }
}
