package com.braintreepayments.api;

public class SignatureVerificationUnitTestUtils {

    public static void disableSignatureVerification() {
        SignatureVerification.enableSignatureVerification = false;
    }
}