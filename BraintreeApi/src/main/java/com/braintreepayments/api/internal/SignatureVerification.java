package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SignatureVerification {

    /**
     * Used to disable signature verification for development and test.
     */
    protected static boolean sEnableSignatureVerification = true;

    /**
     * Check if an app has the correct, matching, signature. Used to prevent malicious apps from
     * impersonating other apps.
     *
     * See <a href="http://stackoverflow.com/questions/16303549/how-can-i-verify-whether-another-app-on-the-system-is-genuine">reference</a>
     *
     * @param context
     * @param packageName the package name of the app to verify.
     * @param certificateSubject the expected certificate subject of the app.
     * @param certificateIssuer the expected certificate issuer of the app.
     * @param publicKeyHashCode the hash code of the app's public key.
     * @return true is signature is valid or signature verification has been disabled.
     */
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

        for (Signature signature : signatures) {
            try {
                byte[] rawCert = signature.toByteArray();
                InputStream certStream = new ByteArrayInputStream(rawCert);

                X509Certificate x509Cert =
                        (X509Certificate) CertificateFactory.getInstance("X509")
                                .generateCertificate(certStream);

                String subject = x509Cert.getSubjectX500Principal().getName();
                String issuer = x509Cert.getIssuerX500Principal().getName();
                int actualPublicKeyHashCode = x509Cert.getPublicKey().hashCode();

                return (certificateSubject.equals(subject) &&
                        certificateIssuer.equals(issuer) &&
                        publicKeyHashCode == actualPublicKeyHashCode);
            } catch (CertificateException e) {
                // continue
            }
        }
        return false;
    }
}
