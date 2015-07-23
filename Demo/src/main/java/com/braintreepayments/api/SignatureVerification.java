package com.braintreepayments.api;

public class SignatureVerification {

    public static void disableAppSwitchSignatureVerification() {
        PayPal.sEnableSignatureVerification = false;
        AppSwitch.sEnableSignatureVerification = false;
    }
}
