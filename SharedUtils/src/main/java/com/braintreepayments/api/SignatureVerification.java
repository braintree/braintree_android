package com.braintreepayments.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Base64;

import androidx.annotation.VisibleForTesting;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

class SignatureVerification {

    /**
     * Used to disable signature verification for development and test.
     */
    static boolean enableSignatureVerification = true;

    /**
     * Check if an app has the correct, matching, signature. Used to prevent malicious apps from
     * impersonating other apps.
     *
     * @param context Android Context
     * @param packageName the package name of the app to verify.
     * @param base64EncodedSignature the base64 encoded signature to verify.
     * @return true is signature is valid or signature verification has been disabled.
     */
    static boolean isSignatureValid(Context context, String packageName,
                                    String base64EncodedSignature) {
        return isSignatureValid(context, packageName, base64EncodedSignature , new CertificateHelper());
    }

    @VisibleForTesting
    @SuppressLint("PackageManagerGetSignatures")
    static boolean isSignatureValid(Context context, String packageName,
                                    String base64EncodedSignature, CertificateHelper certificateHelper) {
        if (!enableSignatureVerification) {
            return true;
        }

        PackageManager packageManager = context.getPackageManager();

        Signature[] signatures;
        try {
            signatures = packageManager
                    .getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
        } catch (NameNotFoundException e) {
            return false;
        }

        if (signatures.length == 0) {
            return false;
        }

        for (Signature signature : signatures) {
            String currentSignature;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(certificateHelper.getEncodedCertificate(signature.toByteArray()));
                currentSignature = Base64.encodeToString(md.digest(), Base64.DEFAULT);
            } catch (NoSuchAlgorithmException | CertificateException e) {
                return false;
            }

            boolean validated = base64EncodedSignature.equals(currentSignature);
            if (!validated) {
                return false;
            }
        }

        return true;
    }
}