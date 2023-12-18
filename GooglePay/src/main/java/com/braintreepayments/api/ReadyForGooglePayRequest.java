package com.braintreepayments.api;

/**
 * Optional parameters to use when checking whether Google Pay is supported and set up on the customer's device.
 */
public class ReadyForGooglePayRequest {

    private boolean existingPaymentMethodRequired;

    /**
     * If set to true, then the {@link GooglePayClient#isReadyToPay(android.content.Context, ReadyForGooglePayRequest, GooglePayIsReadyToPayCallback)}
     * method will call the listener with true if the customer is ready to pay with one or more of your
     * supported card networks.
     *
     * @param existingPaymentMethodRequired Indicates whether the customer must already have at least one payment method from your supported
     *              card networks in order to be considered ready to pay with Google Pay
     */
    public void setExistingPaymentMethodRequired(boolean existingPaymentMethodRequired) {
        this.existingPaymentMethodRequired = existingPaymentMethodRequired;
    }

    public boolean isExistingPaymentMethodRequired() {
        return existingPaymentMethodRequired;
    }
}
