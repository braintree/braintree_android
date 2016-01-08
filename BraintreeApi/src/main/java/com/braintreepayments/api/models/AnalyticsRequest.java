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
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

public class AnalyticsRequest {

    @SerializedName("analytics") public Analytics[] mAnalytics;
    @SerializedName("_meta") public Meta mMeta;

    public AnalyticsRequest(Context context, String event, String integrationType) {
        mAnalytics = new Analytics[]{ new Analytics(event) };
        mMeta = new Meta(context, integrationType);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    private class Analytics {

        @SerializedName("kind") private String mKind;

        public Analytics(String event) {
            mKind = event;
        }
    }

    private class Meta {

        @SerializedName("platform") private String mPlatform;
        @SerializedName("platformVersion") private String mPlatformVersion;
        @SerializedName("sdkVersion") private String mSdkVersion;
        @SerializedName("merchantAppId") private String mMerchantAppId;
        @SerializedName("merchantAppName") private String mMerchantAppName;
        @SerializedName("merchantAppVersion") private String mMerchantAppVersion;
        @SerializedName("deviceRooted") private String mDeviceRooted;
        @SerializedName("deviceManufacturer") private String mDeviceManufacturer;
        @SerializedName("deviceModel") private String mDeviceModel;
        @SerializedName("deviceNetworkType") private String mDeviceNetworkType;
        @SerializedName("androidId") private String mAndroidId;
        @SerializedName("deviceAppGeneratedPersistentUuid") private String mDeviceAppGeneratedPersistentUuid;
        @SerializedName("isSimulator") private String mIsSimulator;
        @SerializedName("deviceScreenOrientation") private String mDeviceScreenOrientation;
        @SerializedName("integrationType") private String mIntegrationType;
        @SerializedName("userInterfaceOrientation") private String mUserInterfaceOrientation;

        protected Meta(Context context, String integrationType) {
            ApplicationInfo applicationInfo;
            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            try {
                applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            } catch (final NameNotFoundException e) {
                applicationInfo = null;
            }

            mPlatform = "Android";
            mPlatformVersion = Integer.toString(VERSION.SDK_INT);
            mSdkVersion = BuildConfig.VERSION_NAME;
            mMerchantAppId = packageName;
            mMerchantAppName = getAppName(applicationInfo, packageManager);
            mMerchantAppVersion = getAppVersion(packageManager, packageName);
            mDeviceRooted = isDeviceRooted();
            mDeviceManufacturer = Build.MANUFACTURER;
            mDeviceModel = Build.MODEL;
            mDeviceNetworkType = getNetworkType(context);
            mAndroidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
            mDeviceAppGeneratedPersistentUuid = getUUID(context);
            mIsSimulator = detectEmulator();
            mIntegrationType = integrationType;
            mUserInterfaceOrientation = getUserOrientation(context);
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
            if (connectivityManager.getActiveNetworkInfo() != null) {
                return connectivityManager.getActiveNetworkInfo().getTypeName();
            }
            return "none";
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
