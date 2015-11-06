package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class WalletAppHelper extends GenericAppHelper {
    private static final String TAG = WalletAppHelper.class.getSimpleName();

    private static final String WALLET_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final String WALLET_APP_CERT_SUBJECT = "O=Paypal";
    private static final String WALLET_APP_CERT_ISSUER = "O=Paypal";
    private static final int WALLET_APP_PUBLIC_KEY_HASH_CODE = 34172764;

    /**
     * Creates an intent to the Wallet app with the given name.
     * @return
     */
    protected Intent createSdkIntent(String action, String className) {
        Intent intent = new Intent(action);
        intent.setComponent(ComponentName.unflattenFromString(className));
        intent.setPackage(WALLET_APP_PACKAGE);
        return intent;
    }

    /**
     * Returns true if the authenticator (p2p app) is present, we're in an
     * environment that supports authenticator, and all the right permissions
     * are present in the wallet app.
     *
     * @return
     */
    public boolean isValidP2pMobileAuthenticatorInstalled(Context context,
                                                          boolean isAuthenticatorSecurityEnabled) {
        return isValidGenericAuthenticatorInstalled(context,
                isAuthenticatorSecurityEnabled,
                WALLET_APP_PACKAGE);
    }


    /**
     * This method supports the case where we configure and/or switch the target app package.
     * @param context
     * @param isAuthenticatorSecurityEnabled
     * @param appPackage
     * @return
     */
    public boolean isValidGenericAuthenticatorInstalled(Context context,
                                                        boolean isAuthenticatorSecurityEnabled,
                                                        String appPackage){
        return isValidAuthenticatorInstalled(context,
                isAuthenticatorSecurityEnabled,
                appPackage,
                WALLET_APP_CERT_SUBJECT,
                WALLET_APP_CERT_ISSUER,
                WALLET_APP_PUBLIC_KEY_HASH_CODE);
    }

    public boolean isWalletIntentSafe(Context context, String intentAction, String intentClass) {
        Intent intent = createSdkIntent(intentAction, intentClass);
        return isGenericIntentSafe(context, intent);
    }
}
