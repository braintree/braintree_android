package com.braintreepayments.api.internal;

public class SignatureVerificationUnitTestUtils {

    public static void disableSignatureVerification() {
        SignatureVerification.sEnableSignatureVerification = false;
    }
}