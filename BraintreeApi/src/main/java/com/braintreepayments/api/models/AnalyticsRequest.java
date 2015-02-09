package com.braintreepayments.api.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

public class AnalyticsRequest {

    public Analytics[] analytics;
    public Meta _meta;

    public AnalyticsRequest(Context context, String event, String integrationType) {
        analytics = new Analytics[]{ new Analytics(event) };
        _meta = new Meta(context, integrationType);
    }

    public String toJson() {
        return Utils.getGson().toJson(this);
    }

    private class Analytics {

        private String kind;

        public Analytics(String event) {
            kind = event;
        }
    }

    private class Meta {

        private String platform;
        private String platformVersion;
        private String sdkVersion;
        private String merchantAppId;
        private String merchantAppName;
        private String merchantAppVersion;
        private String deviceRooted;
        private String deviceManufacturer;
        private String deviceModel;
        private String deviceNetworkType;
        private String androidId;
        private String deviceAppGeneratedPersistentUuid;
        private String isSimulator;
        private String deviceScreenOrientation;
        private String integrationType;
        private String userInterfaceOrientation;

        protected Meta(Context context, String integrationType) {
            ApplicationInfo applicationInfo;
            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            try {
                applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            } catch (final NameNotFoundException e) {
                applicationInfo = null;
            }

            platform = "Android";
            platformVersion = Integer.toString(VERSION.SDK_INT);
            sdkVersion = BuildConfig.VERSION_NAME;
            merchantAppId = packageName;
            merchantAppName = getAppName(applicationInfo, packageManager);
            merchantAppVersion = getAppVersion(packageManager, packageName);
            deviceRooted = isDeviceRooted();
            deviceManufacturer = Build.MANUFACTURER;
            deviceModel = Build.MODEL;
            deviceNetworkType = getNetworkType(context);
            androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            deviceAppGeneratedPersistentUuid = getUUID(context);
            isSimulator = detectEmulator();
            this.integrationType = integrationType;
            userInterfaceOrientation = getUserOrientation(context);
        }

        private String getAppName(ApplicationInfo applicationInfo, PackageManager packageManager) {
            if (applicationInfo != null) {
                return (String) packageManager.getApplicationLabel(applicationInfo);
            } else {
                return "ApplicationNameUnknown";
            }
        }

        private String getAppVersion(PackageManager packageManager, String packageName) {
            try {
                return packageManager.getPackageInfo(packageName, 0).versionName;
            } catch (NameNotFoundException e) {
                return "VersionUnknown";
            }
        }

        private String isDeviceRooted() {
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
                Process process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
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

        private String getNetworkType(Context context) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            return connectivityManager.getActiveNetworkInfo().getTypeName();
        }

        private String getUUID(Context context) {
            SharedPreferences prefs =
                    context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);

            String uuid = prefs.getString("braintreeUUID", null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString().replace("-", "");
                prefs.edit().putString("braintreeUUID", uuid).apply();
            }

            return uuid;
        }

        private String detectEmulator() {
            if ("google_sdk".equalsIgnoreCase(Build.PRODUCT) ||
                    "sdk".equalsIgnoreCase(Build.PRODUCT) ||
                    "Genymotion".equalsIgnoreCase(Build.MANUFACTURER)) {
                return "true";
            } else {
                return "false";
            }
        }

        private String getUserOrientation(Context context) {
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
}
