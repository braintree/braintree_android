package com.braintreepayments.demo;

public enum DemoFeature {
    CREDIT_OR_DEBIT_CARDS(0),
    PAYPAL(1),
    VENMO(2),
    GOOGLE_PAY(3),
    SAMSUNG_PAY(4),
    VISA_CHECKOUT(5),
    LOCAL_PAYMENT(6),
    PREFERRED_PAYMENT_METHODS(7);

    private final int value;

    DemoFeature(int value) {
        this.value = value;
    }

    public static DemoFeature from(int value) {
        for (DemoFeature demoFeature : DemoFeature.values()) {
            if (demoFeature.value == value) {
                return demoFeature;
            }
        }
        return null;
    }
}