package com.paypal.android.sdk.onetouch.core.sdk;

import android.content.Context;
import android.content.Intent;

import java.util.Collection;
import java.util.HashSet;

public class V2WalletHelper extends WalletAppHelper{

    private static final String V2_TOUCH_SDK_INTENT =
            "com.paypal.android.lib.authenticator.activity.v2.TouchActivity";


    private static final Collection<String> V2_AUTHENTICATOR_SCOPES = new HashSet<String>(){{
        add(PayPalScope.FUTURE_PAYMENTS.getScopeUri());
        add(PayPalScope.EMAIL.getScopeUri());
        add(PayPalScope.ADDRESS.getScopeUri());
        add(PayPalScope.PHONE.getScopeUri());
        add(PayPalScope.OPENID.getScopeUri());
    }};


    public Intent getPayPalTouchIntent() {
        Intent intent = createSdkIntent(V2_TOUCH_SDK_INTENT, V2_TOUCH_SDK_INTENT);
        return intent;
    }

    public boolean isValidV1Scope(String scope) {
        if (!V2_AUTHENTICATOR_SCOPES.contains(scope)) {
            return true;
        }
        return false;
    }

    public boolean isValidV2TouchAuthenticatorInstalled(Context context,
                                                        boolean isAuthenticatorSecurityEnabled) {

        boolean isConfiguredToAcceptIntent = isWalletIntentSafe(
                context,
                V2_TOUCH_SDK_INTENT,
                V2_TOUCH_SDK_INTENT);

        return isValidP2pMobileAuthenticatorInstalled(context,
                isAuthenticatorSecurityEnabled)
                && isConfiguredToAcceptIntent;
    }

}
