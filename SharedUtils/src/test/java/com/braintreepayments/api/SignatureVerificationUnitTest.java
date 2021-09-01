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

    @Before
    public void beforeEach() throws PackageManager.NameNotFoundException {
        context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        PackageInfo packageInfo = mock(PackageInfo.class);
        Signature[] signatures = new Signature[123];
        Signature signature = mock(Signature.class);
        byte[] bytes = "example-signature".getBytes();
        signatures[0] = signature;

        when(signature.toByteArray()).thenReturn(bytes);
        packageInfo.signatures = signatures;
        when(packageManager.getPackageInfo(eq("com.example"), eq(PackageManager.GET_SIGNATURES))).thenReturn(packageInfo);
        when(context.getPackageManager()).thenReturn(packageManager);
    }

    @Test
    public void isSignatureValid_whenEncodedSignaturesMatch_returnsTrue() throws NoSuchAlgorithmException {
        Signature signatureToVerify = mock(Signature.class);
        when(signatureToVerify.toByteArray()).thenReturn("example-signature".getBytes());
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(signatureToVerify.toByteArray());

        String base64EncodedSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT);

       assertTrue(SignatureVerification.isSignatureValid(context, "com.example", base64EncodedSignature));
    }

    @Test
    public void isSignatureValid_whenEncodedSignaturesDoNotMatch_returnsFalse() throws NoSuchAlgorithmException {
        Signature signatureToVerify = mock(Signature.class);
        when(signatureToVerify.toByteArray()).thenReturn("different-signature".getBytes());
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(signatureToVerify.toByteArray());

        String base64EncodedSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT);

        assertFalse(SignatureVerification.isSignatureValid(context, "com.example", base64EncodedSignature));
    }

    @Test
    public void isSignatureValid_whenAdditionalSignaturesDoNotMatch_returnsFalse() throws NoSuchAlgorithmException, PackageManager.NameNotFoundException {
        Signature[] signatures = new Signature[123];
        Signature firstSignature = mock(Signature.class);
        byte[] firstSignatureBytes = "example-signature1".getBytes();
        when(firstSignature.toByteArray()).thenReturn(firstSignatureBytes);
        signatures[0] = firstSignature;

        Signature additionalSignature = mock(Signature.class);
        byte[] additionalSignatureBytes = "example-signature2".getBytes();
        when(additionalSignature.toByteArray()).thenReturn(additionalSignatureBytes);
        signatures[1] = additionalSignature;

        Signature signatureToVerify = mock(Signature.class);
        when(signatureToVerify.toByteArray()).thenReturn("example-signature1".getBytes());
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(signatureToVerify.toByteArray());

        String base64EncodedSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT);

        assertFalse(SignatureVerification.isSignatureValid(context, "com.example", base64EncodedSignature));
    }
}