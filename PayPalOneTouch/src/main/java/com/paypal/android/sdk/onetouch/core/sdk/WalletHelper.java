package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.braintreepayments.api.internal.AppHelper;
import com.braintreepayments.api.internal.SignatureVerification;

public class WalletHelper {

    static final String WALLET_APP_PACKAGE = "com.paypal.android.p2pmobile";

    private static final String WALLET_APP_CERT_SUBJECT = "O=Paypal";
    private static final String WALLET_APP_CERT_ISSUER = "O=Paypal";
    private static final int WALLET_APP_PUBLIC_KEY_HASH_CODE = 34172764;

    private static final String V1_TOUCH_SDK_ACTION =
            "com.paypal.android.lib.authenticator.activity.v1.TouchActivity";
    private static final String V1_TOUCH_SDK_COMPONENT =
            "com.paypal.android.p2pmobile/com.paypal.android.lib.authenticator.activity.v1.TouchActivity";
    private static final String V2_TOUCH_SDK_ACTION =
            "com.paypal.android.lib.authenticator.activity.v2.TouchActivity";
    private static final String V2_TOUCH_SDK_COMPONENT =
            "com.paypal.android.p2pmobile/com.paypal.android.lib.authenticator.activity.v2.TouchActivity";
    private static final String V3_TOUCH_SDK_ACTION =
            "com.paypal.android.lib.authenticator.activity.v3.TouchActivity";
    private static final String V3_TOUCH_SDK_COMPONENT =
            "com.paypal.android.p2pmobile/com.paypal.android.lib.authenticator.activity.v3.TouchActivity";

    public static boolean isValidV1TouchAuthenticatorInstalled(Context context,
            boolean isAuthenticatorSecurityEnabled) {
        boolean isIntentAvailable = AppHelper.isIntentAvailable(context,
                createIntent(V1_TOUCH_SDK_ACTION, V1_TOUCH_SDK_COMPONENT, WALLET_APP_PACKAGE));
        return isIntentAvailable &&
                isSignatureValid(context, WALLET_APP_PACKAGE, isAuthenticatorSecurityEnabled);
    }

    public static boolean isValidV2TouchAuthenticatorInstalled(Context context,
            boolean isAuthenticatorSecurityEnabled) {
        boolean isIntentAvailable = AppHelper.isIntentAvailable(context,
                createIntent(V2_TOUCH_SDK_ACTION, V2_TOUCH_SDK_COMPONENT, WALLET_APP_PACKAGE));
        return isIntentAvailable &&
                isSignatureValid(context, WALLET_APP_PACKAGE, isAuthenticatorSecurityEnabled);
    }

    public static boolean isValidV3TouchAuthenticatorInstalled(Context context,
            boolean isAuthenticatorSecurityEnabled) {
        boolean isIntentAvailable = AppHelper.isIntentAvailable(context,
                createIntent(V3_TOUCH_SDK_ACTION, V3_TOUCH_SDK_COMPONENT, WALLET_APP_PACKAGE));
        return isIntentAvailable &&
                isSignatureValid(context, WALLET_APP_PACKAGE, isAuthenticatorSecurityEnabled);
    }

    public static boolean isSignatureValid(Context context, String packageName,
            boolean securityEnabled) {
        return !securityEnabled || SignatureVerification.isSignatureValid(context,
                packageName, WALLET_APP_CERT_SUBJECT, WALLET_APP_CERT_ISSUER,
                WALLET_APP_PUBLIC_KEY_HASH_CODE);
    }

    public static Intent createIntent(String action, String componentName, String packageName) {
        return new Intent(action)
                .setComponent(ComponentName.unflattenFromString(componentName))
                .setPackage(packageName);
    }
}
