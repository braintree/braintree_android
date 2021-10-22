package com.braintreepayments.api;

import static android.content.res.Configuration.ORIENTATION_UNDEFINED;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private final ClassHelper classHelper;

    DeviceInspector() {
        this(new AppHelper(), new ClassHelper());
    }

    @VisibleForTesting
    DeviceInspector(AppHelper appHelper, ClassHelper classHelper) {
        this.appHelper = appHelper;
        this.classHelper = classHelper;
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

    DeviceMetadata getDeviceMetadata(Context context) {
        String networkType = getNetworkType(context);
        String userOrientation = getUserOrientation(context);
        String appVersion = getAppVersion(context);
        String dropInVersion = getDropInVersion();
        boolean isPayPalInstalled = isPayPalInstalled(context);
        boolean isVenmoInstalled = isVenmoInstalled(context);

        return new DeviceMetadata(
            appVersion,
            dropInVersion,
            networkType,
            userOrientation,
            isPayPalInstalled,
            isVenmoInstalled
        );
    }

    private String getNetworkType(Context context) {
        String networkType = null;
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                networkType = networkInfo.getTypeName();
            }
            if (networkType == null) {
                networkType = "none";
            }
        }
        return networkType;
    }

    private String getAppVersion(Context context) {
        String result = "VersionUnknown";
        if (context != null) {
            try {
                result = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ignored) { /* do nothing */ }
        }
        return result;
    }

    private String getUserOrientation(Context context) {
        int orientation = ORIENTATION_UNDEFINED;
        if (context != null) {
            orientation = context.getResources().getConfiguration().orientation;
        }

        switch (orientation) {
            case android.content.res.Configuration.ORIENTATION_PORTRAIT:
                return "Portrait";
            case Configuration.ORIENTATION_LANDSCAPE:
                return "Landscape";
            default:
                return "Unknown";
        }
    }

    /**
     * Gets the current Drop-in version or null.
     *
     * @return string representation of the current Drop-in version, or null if
     * Drop-in is unavailable
     */
    private String getDropInVersion() {
        return classHelper.getFieldValue(
                "com.braintreepayments.api.dropin.BuildConfig",
                "VERSION_NAME"
        );
    }
}
