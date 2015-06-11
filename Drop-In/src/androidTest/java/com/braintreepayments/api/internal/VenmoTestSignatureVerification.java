package com.braintreepayments.api.internal;

public class VenmoTestSignatureVerification {

    public static void disableSignatureVerification() {
        SignatureVerification.sEnableSignatureVerification = false;
    }
}
