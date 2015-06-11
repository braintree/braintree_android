package com.braintreepayments.api.internal;

public class VenmoSignatureVerification {

    public static void disableAppSwitchSignatureVerification() {
        SignatureVerification.sEnableSignatureVerification = false;
    }
}
