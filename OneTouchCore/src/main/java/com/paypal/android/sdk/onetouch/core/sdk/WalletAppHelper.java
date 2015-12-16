package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;

import com.braintreepayments.api.internal.SignatureVerification;

import java.util.List;

public class WalletAppHelper {

    private static final String WALLET_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final String WALLET_APP_CERT_SUBJECT = "O=Paypal";
    private static final String WALLET_APP_CERT_ISSUER = "O=Paypal";
    private static final int WALLET_APP_PUBLIC_KEY_HASH_CODE = 34172764;

    /**
     * Creates an intent to the Wallet app with the given name.
     *
     * @return
     */
    static Intent createSdkIntent(String action, String className) {
        Intent intent = new Intent(action);
        intent.setComponent(ComponentName.unflattenFromString(className));
        intent.setPackage(WALLET_APP_PACKAGE);
        return intent;
    }

    /**
     * Returns true if the authenticator (p2p app) is present, we're in an environment that supports
     * authenticator, and all the right permissions are present in the wallet app.
     *
     * @return
     */
    static boolean isValidP2pMobileAuthenticatorInstalled(Context context,
            boolean isAuthenticatorSecurityEnabled) {
        return isValidGenericAuthenticatorInstalled(context, isAuthenticatorSecurityEnabled,
                WALLET_APP_PACKAGE);
    }

    /**
     * This method supports the case where we configure and/or switch the target app package.
     *
     * @param context
     * @param isAuthenticatorSecurityEnabled
     * @param appPackage
     * @return {@code true} if the authenticator (p2p app) is present, we're in an environment that
     * supports authenticator, and all the right permissions are present in the wallet app.
     * Otherwise returns {@code false}.
     */
    public static boolean isValidGenericAuthenticatorInstalled(Context context,
            boolean isAuthenticatorSecurityEnabled, String appPackage) {
        return (isAppInstalled(context, appPackage) && (!isAuthenticatorSecurityEnabled ||
                SignatureVerification.isSignatureValid(context, appPackage, WALLET_APP_CERT_SUBJECT,
                        WALLET_APP_CERT_ISSUER, WALLET_APP_PUBLIC_KEY_HASH_CODE)));
    }

    public static boolean isWalletIntentSafe(Context context, String intentAction, String intentClass) {
        Intent intent = createSdkIntent(intentAction, intentClass);
        List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(intent, 0);
        return (null != activities) && activities.size() > 0;
    }

    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }
}
