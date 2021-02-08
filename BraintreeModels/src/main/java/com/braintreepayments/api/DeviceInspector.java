package com.braintreepayments.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.VisibleForTesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class DeviceInspector {

    private static final String PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final String VENMO_APP_PACKAGE = "com.venmo";

    private static final String VENMO_APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    private static final String VENMO_CERTIFICATE_SUBJECT = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";
    private static final String VENMO_CERTIFICATE_ISSUER = "CN=Andrew Kortina,OU=Engineering,O=Venmo,L=Philadelphia,ST=PA,C=US";

    private static final int VENMO_PUBLIC_KEY_HASH_CODE = -129711843;

    private final AppHelper appHelper;
    private final ManifestValidator manifestValidator;

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

    public boolean isDeviceEmulator() {
        return isDeviceEmulator(Build.PRODUCT, Build.MANUFACTURER, Build.FINGERPRINT);
    }

    @VisibleForTesting
    boolean isDeviceEmulator(String buildProduct, String buildManufacturer, String buildFingerprint) {
        if ("google_sdk".equalsIgnoreCase(buildProduct) ||
                "sdk".equalsIgnoreCase(buildProduct) ||
                "Genymotion".equalsIgnoreCase(buildManufacturer) ||
                buildFingerprint.contains("generic")) {
            return true;
        } else {
            return false;
        }
    }

    public String getAppName(Context context) {
        ApplicationInfo applicationInfo;
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }

        String appName = null;
        if (applicationInfo != null) {
            appName = (String) packageManager.getApplicationLabel(applicationInfo);
        }

        if (appName == null) {
            return "ApplicationNameUnknown";
        }
        return appName;
    }

    public boolean isDeviceRooted() {
        return isDeviceRooted(
                android.os.Build.TAGS, new File("/system/app/Superuser.apk"), Runtime.getRuntime());
    }

    @VisibleForTesting
    boolean isDeviceRooted(String buildTags, File superUserApkFile, Runtime runtime) {
        boolean check1 = buildTags != null && buildTags.contains("test-keys");

        boolean check2;
        try {
            check2 = superUserApkFile.exists();
        } catch (Exception e) {
            check2 = false;
        }

        boolean check3;
        try {
            Process process = runtime.exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            check3 = in.readLine() != null;
        } catch (Exception e) {
            check3 = false;
        }

        return (check1 || check2 || check3);
    }
}
