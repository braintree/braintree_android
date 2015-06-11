package com.braintreepayments.api;

public class PayPalTestSignatureVerification {

    public static void disableAppSwitchSignatureVerification() {
        PayPal.sEnableSignatureVerification = false;
    }
}
