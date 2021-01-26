package com.braintreepayments.api;

import com.braintreepayments.api.SignatureVerification;

public class SignatureVerificationUnitTestUtils {

    public static void disableSignatureVerification() {
        SignatureVerification.sEnableSignatureVerification = false;
    }
}