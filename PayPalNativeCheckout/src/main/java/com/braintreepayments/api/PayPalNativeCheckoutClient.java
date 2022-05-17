package com.braintreepayments.api;

public class PayPalNativeCheckoutClient {

    private PayPalNativeCheckoutListener listener;

    public void tokenize(PayPalNativeCheckoutRequest request) {
        // TODO: start native checkout
    }

     public void setListener(PayPalNativeCheckoutListener listener) {
        this.listener = listener;
     }
}
