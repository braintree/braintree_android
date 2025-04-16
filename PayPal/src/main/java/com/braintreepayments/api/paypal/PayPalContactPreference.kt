package com.braintreepayments.api.paypal

/**
 * Contact information section preference within the payment flow
 */
enum class PayPalContactPreference(
    internal val stringValue: String
) {
    /**
     * Disables the contact information section in the payment flow
     */
    NO_CONTACT_INFORMATION("NO_CONTACT_INFO"),

    /**
     * Enables the contact information section but disables the buyer's ability to update the contact information
     */
    RETAIN_CONTACT_INFORMATION("RETAIN_CONTACT_INFO"),

    /**
     * Enables the contact information section and enables the buyer's ability to update the contact information
     */
    UPDATE_CONTACT_INFORMATION("UPDATE_CONTACT_INFO"),
}
