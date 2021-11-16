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

class SignatureVerifier {

    private final CertificateHelper certificateHelper;

    SignatureVerifier() {
        this(new CertificateHelper());
    }

    @VisibleForTesting
    SignatureVerifier(CertificateHelper certificateHelper) {
        this.certificateHelper = certificateHelper;
    }

    /**
     * Check if an app has the correct, matching, signature. Used to prevent malicious apps from
     * impersonating other apps.
     *
     * @param context                Android Context
     * @param packageName            the package name of the app to verify.
     * @param base64EncodedSignature the base64 encoded signature to verify.
     * @return true is signature is valid or signature verification has been disabled.
     */
    @SuppressLint("PackageManagerGetSignatures")
    boolean isSignatureValid(Context context, String packageName, String base64EncodedSignature) {

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
                byte[] encodedCert = certificateHelper.getEncodedCertificate(signature.toByteArray());
                currentSignature = Base64.encodeToString(md.digest(encodedCert), Base64.DEFAULT);
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