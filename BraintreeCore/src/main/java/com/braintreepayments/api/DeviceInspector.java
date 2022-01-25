package com.braintreepayments.api;

import static android.content.res.Configuration.ORIENTATION_UNDEFINED;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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

    @VisibleForTesting
    static final String VENMO_BASE_64_ENCODED_SIGNATURE = "x34mMawEUcCG8l95riWCOK+kAJYejVmdt44l6tzcyUc=\n";

    private final AppHelper appHelper;
    private final ClassHelper classHelper;
    private final UUIDHelper uuidHelper;
    private final Runtime runtime;
    private final File superUserApkFile;
    private final SignatureVerifier signatureVerifier;

    DeviceInspector() {
        this(
                new AppHelper(),
                new ClassHelper(),
                new UUIDHelper(),
                new SignatureVerifier(),
                Runtime.getRuntime(),
                new File("/system/app/Superuser.apk")
        );
    }

    @VisibleForTesting
    DeviceInspector(AppHelper appHelper, ClassHelper classHelper, UUIDHelper uuidHelper, SignatureVerifier signatureVerifier, Runtime runtime, File superUserApkFile) {
        this.appHelper = appHelper;
        this.classHelper = classHelper;
        this.uuidHelper = uuidHelper;
        this.signatureVerifier = signatureVerifier;
        this.runtime = runtime;
        this.superUserApkFile = superUserApkFile;
    }

    DeviceMetadata getDeviceMetadata(Context context, String sessionId, String integration) {
        String buildTags = android.os.Build.TAGS;
        return getDeviceMetadata(context, sessionId, integration, buildTags);
    }

    @VisibleForTesting
    DeviceMetadata getDeviceMetadata(Context context, String sessionId, String integration, String buildTags) {
        return new DeviceMetadata.Builder()
                .platform("Android")
                .platformVersion(Integer.toString(Build.VERSION.SDK_INT))
                .sdkVersion(BuildConfig.VERSION_NAME)
                .merchantAppId(context.getPackageName())
                .merchantAppName(getAppName(context))
                .deviceManufacturer(Build.MANUFACTURER)
                .deviceModel(Build.MODEL)
                .devicePersistentUUID(uuidHelper.getPersistentUUID(context))
                .isSimulator(isDeviceEmulator())
                .sessionId(sessionId)
                .integration(integration)
                .networkType(getNetworkType(context))
                .userOrientation(getUserOrientation(context))
                .appVersion(getAppVersion(context))
                .dropInVersion(getDropInVersion())
                .isPayPalInstalled(isPayPalInstalled(context))
                .isVenmoInstalled(isVenmoInstalled(context))
                .build();
    }

    /**
     * @param context A context to access the installed packages.
     * @return boolean depending on if the Venmo app is installed, and has a valid signature.
     */
    boolean isVenmoAppSwitchAvailable(Context context) {
        boolean isVenmoIntentAvailable = appHelper.isIntentAvailable(context, getVenmoIntent());
        boolean isVenmoSignatureValid = signatureVerifier.isSignatureValid(
                context, VENMO_APP_PACKAGE, VENMO_BASE_64_ENCODED_SIGNATURE);
        return isVenmoIntentAvailable && isVenmoSignatureValid;
    }

    boolean isPayPalInstalled(Context context) {
        return appHelper.isAppInstalled(context, PAYPAL_APP_PACKAGE);
    }

    boolean isVenmoInstalled(Context context) {
        return appHelper.isAppInstalled(context, VENMO_APP_PACKAGE);
    }

    private static Intent getVenmoIntent() {
        return new Intent().setComponent(new ComponentName(VENMO_APP_PACKAGE, VENMO_APP_PACKAGE + "." + VENMO_APP_SWITCH_ACTIVITY));
    }

    private boolean isDeviceEmulator() {
        return "google_sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "Genymotion".equalsIgnoreCase(Build.MANUFACTURER) ||
                Build.FINGERPRINT.contains("generic");
    }

    private String getAppName(Context context) {
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

    private String getNetworkType(Context context) {
        String networkType = null;
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                networkType = networkInfo.getTypeName();
            }
        }
        if (networkType == null) {
            networkType = "none";
        }
        return networkType;
    }

    private String getAppVersion(Context context) {
        String result = "VersionUnknown";
        if (context != null) {
            try {
                PackageInfo packageInfo =
                        context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                if (packageInfo != null) {
                    result = packageInfo.versionName;
                }
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
