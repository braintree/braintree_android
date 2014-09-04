package com.braintreepayments.api;

public class SignatureVerification {

    public static void disableAppSwitchSignatureVerification() {
        PayPalHelper.sEnableSignatureVerification = false;
        AppSwitch.sDisableSignatureVerification = true;
    }
}
