package com.braintreepayments.api.internal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SignatureVerification {

    /**
     * Used to disable signature verification for development and test.
     */
    static boolean sEnableSignatureVerification = true;

    /**
     * Check if an app has the correct, matching, signature. Used to prevent malicious apps from
     * impersonating other apps.
     *
     * @deprecated
     * @param context
     * @param packageName the package name of the app to verify.
     * @param certificateSubject the expected certificate subject of the app.
     * @param certificateIssuer the expected certificate issuer of the app.
     * @param publicKeyHashCode the hash code of the app's public key.
     * @return true is signature is valid or signature verification has been disabled.
     */
    @Deprecated
    @SuppressLint("PackageManagerGetSignatures")
    public static boolean isSignatureValid(Context context, String packageName,
            String certificateSubject, String certificateIssuer, int publicKeyHashCode) {
        if (!sEnableSignatureVerification) {
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

        InputStream certStream = null;
        boolean validated = (signatures.length != 0);
        for (Signature signature : signatures) {
            try {
                certStream = new ByteArrayInputStream(signature.toByteArray());

                X509Certificate x509Cert =
                        (X509Certificate) CertificateFactory.getInstance("X509")
                                .generateCertificate(certStream);

                String subject = x509Cert.getSubjectX500Principal().getName();
                String issuer = x509Cert.getIssuerX500Principal().getName();
                int actualPublicKeyHashCode = x509Cert.getPublicKey().hashCode();

                validated &= (certificateSubject.equals(subject) &&
                        certificateIssuer.equals(issuer) &&
                        publicKeyHashCode == actualPublicKeyHashCode);

                if (!validated) {
                    return false;
                }
            } catch (CertificateException e) {
                return false;
            } finally {
                try {
                    if (certStream != null) {
                        certStream.close();
                    }
                } catch(IOException ignored) {}
            }
        }

        return validated;
    }

    /**
     * Check if an app has the correct, matching, signature. Used to prevent malicious apps from
     * impersonating other apps.
     *
     * @param context Android Context
     * @param packageName the package name of the app to verify.
     * @param base64EncodedSignature the base64 encoded signature to verify.
     * @return true is signature is valid or signature verification has been disabled.
     */
    public static boolean isSignatureValid(Context context, String packageName,
                                    String base64EncodedSignature) {
        return isSignatureValid(context, packageName, base64EncodedSignature , new CertificateHelper());
    }

    @VisibleForTesting
    @SuppressLint("PackageManagerGetSignatures")
    static boolean isSignatureValid(Context context, String packageName,
                                    String base64EncodedSignature, CertificateHelper certificateHelper) {
        if (!sEnableSignatureVerification) {
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
