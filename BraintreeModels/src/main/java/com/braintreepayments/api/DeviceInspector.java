package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.internal.AppHelper;
import com.braintreepayments.api.internal.ManifestValidator;
import com.braintreepayments.api.internal.SignatureVerification;
import com.braintreepayments.api.models.Configuration;

import java.util.HashMap;
import java.util.Map;

public class DeviceInspector {

    private static final String PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final String VENMO_APP_PACKAGE = "com.venmo";

    private static final String VENMO_APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    private static final String VENMO_CERTIFICATE_SUBJECT = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    private static final String VENMO_CERTIFICATE_ISSUER = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";

    private static final int VENMO_PUBLIC_KEY_HASH_CODE = -129711843;

    private AppHelper appHelper;
    private ManifestValidator manifestValidator;

    public DeviceInspector() {
        this(new AppHelper(), new ManifestValidator());
    }

    @VisibleForTesting
    DeviceInspector(AppHelper appHelper, ManifestValidator manifestValidator) {
        this.appHelper = appHelper;
        this.manifestValidator = manifestValidator;
    }

    public boolean isPayPalInstalled(Context context) {
        return appHelper.isAppInstalled(context, PAYPAL_APP_PACKAGE);
    }

    public boolean isVenmoInstalled(Context context) {
        return appHelper.isAppInstalled(context, VENMO_APP_PACKAGE);
    }

    /**
     * @param context A context to access the installed packages.
     * @return boolean depending on if the Venmo app is installed, and has a valid signature.
     */
    public boolean isVenmoAppSwitchAvailable(Context context) {
        return appHelper.isIntentAvailable(context, getVenmoIntent()) &&
                SignatureVerification.isSignatureValid(context, VENMO_APP_PACKAGE, VENMO_CERTIFICATE_SUBJECT, VENMO_CERTIFICATE_ISSUER,
                        VENMO_PUBLIC_KEY_HASH_CODE);
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(VENMO_APP_PACKAGE, VENMO_APP_PACKAGE + "." + VENMO_APP_SWITCH_ACTIVITY));
    }

    public boolean canBrowserSwitch(Context context, String returnUrlScheme) {
        return manifestValidator.isUrlSchemeDeclaredInAndroidManifest(context,
                returnUrlScheme, BraintreeBrowserSwitchActivity.class);
    }
}
