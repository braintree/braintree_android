package com.braintreepayments.api.paypal

/**
 * The call-to-action in the PayPal Vault flow
 */
enum class PayPalVaultUserAction(internal val stringValue: String) {

    /**
     * Shows the default call-to-action text on the PayPal Vault page. By default the final button
     * will show the localized word for "Save and Continue" and implies that the final
     * amount billed is not yet known.
     */
    USER_ACTION_DEFAULT(""),

    /**
     * Shows a deterministic call-to-action. Changes the button text to "Setup Now", conveying to
     * the user that the funding instrument will be set up for future payments.
     */
    USER_ACTION_SETUP_NOW("setup_now"),
}
