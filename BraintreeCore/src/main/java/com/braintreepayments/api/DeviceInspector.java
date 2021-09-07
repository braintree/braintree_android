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

class DeviceInspector {

    private static final String PAYPAL_APP_PACKAGE = "com.paypal.android.p2pmobile";
    private static final String VENMO_APP_PACKAGE = "com.venmo";

    private static final String VENMO_APP_SWITCH_ACTIVITY = "controller.SetupMerchantActivity";
    private static final String VENMO_BASE_64_ENCODED_SIGNATURE = "x34mMawEUcCG8l95riWCOK+kAJYejVmdt44l6tzcyUc=\n";

    private final AppHelper appHelper;

    DeviceInspector() {
        this(new AppHelper());
    }

    @VisibleForTesting
    DeviceInspector(AppHelper appHelper) {
        this.appHelper = appHelper;
    }

    boolean isPayPalInstalled(Context context) {
        return appHelper.isAppInstalled(context, PAYPAL_APP_PACKAGE);
    }

    boolean isVenmoInstalled(Context context) {
        return appHelper.isAppInstalled(context, VENMO_APP_PACKAGE);
    }

    /**
     * @param context A context to access the installed packages.
     * @return boolean depending on if the Venmo app is installed, and has a valid signature.
     */
    boolean isVenmoAppSwitchAvailable(Context context) {
        return appHelper.isIntentAvailable(context, getVenmoIntent()) &&
                SignatureVerification.isSignatureValid(context, VENMO_APP_PACKAGE,
                        VENMO_BASE_64_ENCODED_SIGNATURE);
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(VENMO_APP_PACKAGE, VENMO_APP_PACKAGE + "." + VENMO_APP_SWITCH_ACTIVITY));
    }

    boolean isDeviceEmulator() {
        return isDeviceEmulator(Build.PRODUCT, Build.MANUFACTURER, Build.FINGERPRINT);
    }

    @VisibleForTesting
    boolean isDeviceEmulator(String buildProduct, String buildManufacturer, String buildFingerprint) {
        return "google_sdk".equalsIgnoreCase(buildProduct) ||
                "sdk".equalsIgnoreCase(buildProduct) ||
                "Genymotion".equalsIgnoreCase(buildManufacturer) ||
                buildFingerprint.contains("generic");
    }

    String getAppName(Context context) {
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

    boolean isDeviceRooted() {
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
