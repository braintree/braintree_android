package com.braintreepayments.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * Contains a cornucopia of information about the device.
 */
public class DeviceUtils {

    private static final String BRAINTREE_UUID_KEY = "braintreeUUID";

    /**
     * Returns the application name.
     */
    protected static String getAppName(ApplicationInfo applicationInfo,
            PackageManager packageManager) {
        if (applicationInfo != null) {
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } else {
            return "ApplicationNameUnknown";
        }
    }

    /**
     * Returns the version name.
     */
    protected static String getAppVersion(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "VersionUnknown";
        }
    }

    /**
     * Returns a String representation of whether the device is rooted.
     */
    protected static String isDeviceRooted() {
        String buildTags = android.os.Build.TAGS;
        boolean check1 = buildTags != null && buildTags.contains("test-keys");

        boolean check2;
        try {
            File file = new File("/system/app/Superuser.apk");
            check2 = file.exists();
        } catch (Exception e) {
            check2 = false;
        }

        boolean check3;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) {
                check3 = true;
            } else {
                check3 = false;
            }
        } catch (Exception e) {
            check3 = false;
        }

        return Boolean.toString(check1 || check2 || check3);
    }

    /**
     * Returns the current network type.
     * @param context
     * @return
     */
    protected static String getNetworkType(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo().getTypeName();
    }

    protected static String getUUID(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);

        String uuid = prefs.getString(BRAINTREE_UUID_KEY, null);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString().replace("-", "");
            prefs.edit().putString(BRAINTREE_UUID_KEY, uuid).apply();
        }

        return uuid;
    }

    protected static String detectEmulator() {
        if ("google_sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "Genymotion".equalsIgnoreCase(Build.MANUFACTURER) ||
                Build.FINGERPRINT.contains("generic")) {
            return "true";
        } else {
            return "false";
        }
    }

    protected static String getUserOrientation(Context context) {
        int orientation = context.getResources().getConfiguration().orientation;
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return "Portrait";
            case Configuration.ORIENTATION_LANDSCAPE:
                return "Landscape";
            default:
                return "Unknown";
        }
    }
}
