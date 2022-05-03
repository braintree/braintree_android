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
     * {@link true} if the user confirms cancellation of the payment flow.
     *
     * {@link false} if the user returns to the app without completing the payment flow and without
     * performing an explicit cancellation action (i.e. presses the back button or closes the
     * browser tab)
     */
    public boolean isExplicitCancellation() {
        return isExplicitCancellation;
    }
}
