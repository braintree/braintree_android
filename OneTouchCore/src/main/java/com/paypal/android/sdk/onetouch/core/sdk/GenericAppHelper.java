package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class GenericAppHelper {
    private static final String TAG = GenericAppHelper.class.getSimpleName();

    public boolean isGenericIntentSafe(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
        boolean isIntentSafe = (null != activities) && activities.size() > 0;
        return isIntentSafe;
    }



    /**
     * Returns true if the authenticator (p2p app) is present, we're in an
     * environment that supports authenticator, and all the right permissions
     * are present in the wallet app.
     *
     * @return
     */
    protected boolean isValidAuthenticatorInstalled(Context context,
                                                    boolean isAuthenticatorSecurityEnabled,
                                                    String packageName,
                                                    String subject,
                                                    String issuer,
                                                    int publicKeyHashCode) {
        boolean isValid = false;

        PackageManager pm = context.getPackageManager();
        try {
            // validate wallet app is present. Will throw NameNotFoundException if not.
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA
                    | PackageManager.GET_PERMISSIONS);

           if (isAuthenticatorSecurityEnabled && !isSignatureValid(pm,
                    packageName,
                    subject,
                    issuer,
                    publicKeyHashCode)) {
            } else {
                // everything valid, yay!
                isValid = true;
            }

        } catch (NameNotFoundException e) {
            // ignore
        }

        return isValid;
    }


    /**
     * Validate wallet app has the correct signature, to prevent malicious
     * wallet apps. See <a href=
     * "http://stackoverflow.com/questions/16303549/how-can-i-verify-whether-another-app-on-the-system-is-genuine"
     * >reference</a>
     *
     * @param pm
     * @return
     * @throws android.content.pm.PackageManager.NameNotFoundException
     */
    private boolean isSignatureValid(PackageManager pm, String packageName, String inSubject, String inIssuer, int inPublicKeyHashCode) throws NameNotFoundException {
        Signature[] sigs =
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
        for (Signature sig : sigs) {
            byte[] rawCert = sig.toByteArray();
            InputStream certStream = new ByteArrayInputStream(rawCert);

            try {
                X509Certificate x509Cert =
                        (X509Certificate) CertificateFactory.getInstance("X509")
                                .generateCertificate(certStream);

                String subject = x509Cert.getSubjectX500Principal().getName();
                String issuer = x509Cert.getIssuerX500Principal().getName();
                int publicKeyHashCode = x509Cert.getPublicKey().hashCode();

                boolean isValidCert =
                        inSubject.equals(subject)
                                && inIssuer.equals(issuer)
                                && inPublicKeyHashCode == publicKeyHashCode;

                return isValidCert;
            } catch (CertificateException e) {
                // ignore
            }
        }
        return false;
    }
}
