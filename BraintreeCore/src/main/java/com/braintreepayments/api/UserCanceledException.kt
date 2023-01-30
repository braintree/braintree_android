package com.braintreepayments.api

/**
 * Error class thrown when a user cancels a payment flow
 */
open class UserCanceledException : BraintreeException {

    internal constructor(message: String?) : super(message)
    internal constructor(message: String?, isExplicitCancelation: Boolean) : super(message) {
        this.isExplicitCancelation = isExplicitCancelation
    }

    /**
     * @return whether or not the user explicitly canceled the payment flow.
     *
     * This value will be true if the user manually confirms cancellation of the payment flow.
     *
     * This value will be false if the user returns to the app without completing the payment flow
     * and the action performed to return to the app is unknown. For browser switching flows, this
     * could mean the user returned to the app through multi-tasking without completing the flow,
     * the user closed the browser tab, or the user pressed the back button.
     */
    var isExplicitCancelation = false
        private set
}