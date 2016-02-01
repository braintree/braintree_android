package com.braintreepayments.api.internal;

public class SignatureVerificationTestUtils {

    public static void disableSignatureVerification() {
        SignatureVerification.sEnableSignatureVerification = false;
    }
}