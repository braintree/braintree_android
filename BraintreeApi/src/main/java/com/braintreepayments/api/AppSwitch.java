package com.braintreepayments.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public abstract class AppSwitch {

    public static final String EXTRA_MERCHANT_ID = "com.braintreepayments.api.MERCHANT_ID";
    public static final String EXTRA_PAYMENT_METHOD_NONCE = "com.braintreepayments.api.EXTRA_PAYMENT_METHOD_NONCE";
    public static final String EXTRA_OFFLINE = "com.braintreepayments.api.OFFLINE";

    protected static boolean sEnableSignatureVerification = true;

    protected Context mContext;
    protected ClientToken mClientToken;

    public AppSwitch(Context context, ClientToken clientToken) {
        mContext = context;
        mClientToken = clientToken;
    }

    protected boolean isAvailable() {
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(getLaunchIntent(), 0);

        return (activities.size() == 1 &&
                getPackage().equals(activities.get(0).activityInfo.packageName) &&
                isSignatureValid());
    }

    /**
     * App has the correct signature, to prevent malicious apps.
     * See <a href="http://stackoverflow.com/questions/16303549/how-can-i-verify-whether-another-app-on-the-system-is-genuine">reference</a>
     *
     * @return true is signature is valid
     */
    private boolean isSignatureValid() {
        if (!sEnableSignatureVerification) {
            return true;
        }

        PackageManager packageManager = mContext.getPackageManager();
        Signature[] signatures;
        try {
            signatures = packageManager
                            .getPackageInfo(getPackage(), PackageManager.GET_SIGNATURES).signatures;
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
                int publicKeyHashCode = x509Cert.getPublicKey().hashCode();

                boolean isValidCert =
                        getCertificateSubject().equals(subject)
                                && getCertificateIssuer().equals(issuer)
                                && getPublicKeyHashCode() == publicKeyHashCode;
                return isValidCert;
            } catch (CertificateException e) {
                // continue
            }
        }
        return false;
    }

    protected Intent getLaunchIntent() {
        return new Intent()
                .setComponent(new ComponentName(
                        getPackage(), getPackage() + "." + getAppSwitchActivity()));
    }

    protected void launch(Activity activity, int requestCode) throws AppSwitchNotAvailableException {
        if (isAvailable()) {
            activity.startActivityForResult(getLaunchIntent(), requestCode);
        } else {
            throw new AppSwitchNotAvailableException();
        }
    }

    protected abstract String getPackage();
    protected abstract String getAppSwitchActivity();
    protected abstract String getCertificateSubject();
    protected abstract String getCertificateIssuer();
    protected abstract int getPublicKeyHashCode();
    protected abstract String handleAppSwitchResponse(int requestCode, Intent data);

}
