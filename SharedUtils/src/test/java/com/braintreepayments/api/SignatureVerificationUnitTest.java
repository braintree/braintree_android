package com.braintreepayments.api;


import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RunWith(RobolectricTestRunner.class)
public class SignatureVerificationUnitTest {

    private Context context;
    private PackageInfo packageInfo;

    @Before
    public void beforeEach() throws PackageManager.NameNotFoundException {
        context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        packageInfo = mock(PackageInfo.class);
        Signature[] signatures = new Signature[1];
        signatures[0] = createMockSignature("example-signature");

        packageInfo.signatures = signatures;
        when(packageManager.getPackageInfo(eq("com.example"), eq(PackageManager.GET_SIGNATURES))).thenReturn(packageInfo);
        when(context.getPackageManager()).thenReturn(packageManager);
    }

    @Test
    public void isSignatureValid_whenEncodedSignaturesMatch_returnsTrue() throws NoSuchAlgorithmException {
        String base64EncodedSignature = base64EncodedSHA256("example-signature");
        assertTrue(SignatureVerification.isSignatureValid(context, "com.example", base64EncodedSignature));
    }

    @Test
    public void isSignatureValid_whenEncodedSignaturesDoNotMatch_returnsFalse() throws NoSuchAlgorithmException {
        String base64EncodedSignature = base64EncodedSHA256("different-signature");
        assertFalse(SignatureVerification.isSignatureValid(context, "com.example", base64EncodedSignature));
    }

    @Test
    public void isSignatureValid_whenAdditionalSignaturesDoNotMatch_returnsFalse() throws NoSuchAlgorithmException {
        Signature[] signatures = new Signature[2];
        signatures[0] = createMockSignature("example-signature1");
        signatures[1] = createMockSignature("example-signature2");

        packageInfo.signatures = signatures;

        String base64EncodedSignature = base64EncodedSHA256("example-signature1");

        assertFalse(SignatureVerification.isSignatureValid(context, "com.example", base64EncodedSignature));
    }

    private static String base64EncodedSHA256(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(input.getBytes());
        return Base64.encodeToString(md.digest(), Base64.DEFAULT);
    }

    private static Signature createMockSignature(String signatureContent) {
        Signature signature = mock(Signature.class);
        byte[] bytes = signatureContent.getBytes();
        when(signature.toByteArray()).thenReturn(bytes);
        return signature;
    }
}