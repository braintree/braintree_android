package com.braintreepayments.api.paypal

/**
 * The call-to-action in the PayPal Checkout flow
 */
enum class PayPalPaymentUserAction(internal val stringValue: String) {

    /**
     * Shows the default call-to-action text on the PayPal Express Checkout page. This option
     * indicates that a final confirmation will be shown on the merchant checkout site before the
     * user's payment method is charged.
     */
    USER_ACTION_DEFAULT(""),

    /**
     * Shows a deterministic call-to-action. This option indicates to the user that their payment
     * method will be charged when they click the call-to-action button on the PayPal Checkout page,
     * and that no final confirmation page will be shown on the merchant's checkout page. This
     * option works for both checkout and vault flows.
     */
    USER_ACTION_COMMIT("commit"),
}
