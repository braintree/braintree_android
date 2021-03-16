package com.braintreepayments.api;

public class PayPalVaultRequest extends PayPalRequest {

    private boolean offerCredit;

    public PayPalVaultRequest() {}

    /**
     * Offers PayPal Credit prominently in the payment flow. Defaults to false. Only available with Billing Agreements
     * and PayPal Checkout.
     *
     * @param offerCredit Whether to offer PayPal Credit.
     */
    public void setOfferCredit(boolean offerCredit) {
        this.offerCredit = offerCredit;
    }

    public boolean shouldOfferCredit() {
        return offerCredit;
    }

}
