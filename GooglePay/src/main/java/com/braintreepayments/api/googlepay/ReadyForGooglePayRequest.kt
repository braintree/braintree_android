package com.braintreepayments.api.googlepay

/**
 * Optional parameters to use when checking whether Google Pay is supported and set up on the customer's device.
 *
 * @property isExistingPaymentMethodRequired Indicates whether the customer must already have
 * 1at least one payment method from your supported card networks in order to be considered
 * ready to pay with Google Pay
 */
data class ReadyForGooglePayRequest(
    var isExistingPaymentMethodRequired: Boolean = false
)
