package com.braintreepayments.api;

/**
 * Error class thrown when a user cancels a payment flow
 */
public class UserCanceledException extends BraintreeException {

    private boolean isExplicitCancellation;

    UserCanceledException(String message) {
        super(message);
    }

    UserCanceledException(String message, boolean isExplicitCancellation) {
        super(message);
        this.isExplicitCancellation = isExplicitCancellation;
    }

    /**
     * @return whether or not the user explicitly canceled the payment flow.
     *
     * {@link true} if the user manually confirms cancellation of the payment flow.
     *
     * {@link false} if the user returns to the app without completing the payment flow and the
     * action performed to return to the app is unknown. For browser switching flows, this could
     * mean the user returned to the app through multi-tasking without completing the flow, the user
     * closed the browser tab, or the user pressed the back button.
     */
    public boolean isExplicitCancellation() {
        return isExplicitCancellation;
    }
}
