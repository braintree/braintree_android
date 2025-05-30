package com.braintreepayments.api.paypal

/**
 * The call-to-action in the PayPal flow
 */
enum class PayPalPaymentUserAction(internal val stringValue: String) {

    /**
     * Shows the default call-to-action text on the PayPal Express Checkout page. This option
     * indicates that a final confirmation will be shown on the merchant checkout site before the
     * user's payment method is charged.
     */
    USER_ACTION_DEFAULT(""),

    /**
     * Shows a deterministic call-to-action for the PayPal Checkout flow. This option indicates to
     * the user that their payment method will be charged when they click the call-to-action button
     * on the PayPal Checkout page, and that no final confirmation page will be shown on the
     * merchant's checkout page. This option only works for the PayPal Checkout flow.
     */
    USER_ACTION_COMMIT("commit"),

    /**
     * Shows a deterministic call-to-action for the PayPal Vault flow. Changes the button text
     * to "Setup Now", conveying to the user that the funding instrument will be set up for
     * future payments. This option only works for the PayPal Vault flow.
     */
    USER_ACTION_SETUP_NOW("setup_now"),
}
