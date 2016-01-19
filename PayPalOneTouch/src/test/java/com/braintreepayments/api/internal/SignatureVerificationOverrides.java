package com.braintreepayments.api.internal;

public class SignatureVerificationOverrides {

    public static void disableSignatureVerification(boolean disabled) {
        SignatureVerification.sEnableSignatureVerification = !disabled;
    }
}
