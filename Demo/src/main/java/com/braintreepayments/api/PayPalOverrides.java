package com.braintreepayments.api;

public class PayPalOverrides {

    public static void setFuturePaymentsOverride(boolean useFuturePayments) {
        PayPal.sFuturePaymentsOverride = useFuturePayments;
    }
}
