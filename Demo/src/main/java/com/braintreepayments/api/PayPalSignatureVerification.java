package com.braintreepayments.api;

public class PayPalSignatureVerification {

    public static void disableAppSwitchSignatureVerification() {
        PayPal.sEnableSignatureVerification = false;
    }
}
