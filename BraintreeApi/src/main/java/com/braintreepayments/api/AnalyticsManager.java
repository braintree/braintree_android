package com.braintreepayments.api;

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

import com.braintreepayments.api.interfaces.ConfigurationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Centralized location for caching, queuing, and sending Analytics events
 */
class AnalyticsManager {

    private static final String ANALYTICS_KEY = "analytics";
    private static final String KIND_KEY = "kind";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String META_KEY = "_meta";
    private static final String PLATFORM_KEY = "platform";
    private static final String PLATFORM_VERSION_KEY = "platformVersion";
    private static final String SDK_VERSION_KEY = "sdkVersion";
    private static final String MERCHANT_APP_ID_KEY = "merchantAppId";
    private static final String MERCHANT_APP_NAME_KEY = "merchantAppName";
    private static final String MERCHANT_APP_VERSION_KEY = "merchantAppVersion";
    private static final String DEVICE_ROOTED_KEY = "deviceRooted";
    private static final String DEVICE_MANUFACTURER_KEY = "deviceManufacturer";
    private static final String DEVICE_MODEL_KEY = "deviceModel";
    private static final String DEVICE_NETWORK_TYPE_KEY = "deviceNetworkType";
    private static final String ANDROID_ID_KEY = "androidId";
    private static final String DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY = "deviceAppGeneratedPersistentUuid";
    private static final String IS_SIMULATOR_KEY = "isSimulator";
    private static final String INTEGRATION_TYPE_KEY = "integrationType";
    private static final String USER_INTERFACE_ORIENTATION_KEY = "userInterfaceOrientation";
    private static final String BRAINTREE_UUID_KEY = "braintreeUUID";

    private static final int REQUEST_THRESHOLD = 5;

    private static ArrayList<AnalyticsRequest> sRequestQueue = new ArrayList<>();
    private static JSONObject sCachedMetadata;

    /**
     * Queue or send analytics request with integration type and event.
     *
     * @param integrationType The current method of integration used.
     * @param eventFragment The analytics event to record.
     */
    static void sendRequest(final BraintreeFragment fragment, final String integrationType, final String eventFragment) {
        final AnalyticsRequest request = new AnalyticsRequest(integrationType, eventFragment);
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched() {
                if (!fragment.getConfiguration().getAnalytics().isEnabled()) {
                    return;
                }

                sRequestQueue.add(request);

                if (sRequestQueue.size() >= REQUEST_THRESHOLD) {
                    processRequests(fragment);
                }
            }
        });
    }

    /**
     * Batch and send remaining analytics events even if batch size has not been reached.
     */
    static void flushEvents(final BraintreeFragment fragment) {
        fragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched() {
                if (fragment.getConfiguration().getAnalytics().isEnabled()) {
                    processRequests(fragment);
                }
            }
        });
    }

    private static void processRequests(BraintreeFragment fragment) {
        if (sRequestQueue.size() == 0) {
            return;
        }

        ArrayList<AnalyticsRequest> requests = new ArrayList<>();
        requests.addAll(sRequestQueue);
        sRequestQueue.clear();

        try {
            JSONArray events = new JSONArray();
            for (AnalyticsRequest request : requests) {
                try {
                    JSONObject event = new JSONObject()
                            .put(KIND_KEY, request.getEvent())
                            .put(TIMESTAMP_KEY, request.getTimestamp());
                    events.put(event);
                } catch (JSONException ignored) {}
            }

            JSONObject fullMetaData = generateRequestBody(fragment.getContext(),
                    requests.get(0).getIntegrationType());

            String requestBody = new JSONObject()
                    .put(ANALYTICS_KEY, events)
                    .put(META_KEY, fullMetaData)
                    .toString();

            fragment.getHttpClient().post(fragment.getConfiguration().getAnalytics().getUrl(),
                    requestBody, null);
        } catch (JSONException ignored) {}
    }

    private static JSONObject generateRequestBody(Context context, String integrationType)
            throws JSONException {
        if (sCachedMetadata == null) {
            sCachedMetadata = populateCachedMetadata(context);
        }

        return new JSONObject(sCachedMetadata.toString())
                .put(DEVICE_NETWORK_TYPE_KEY, getNetworkType(context))
                .put(INTEGRATION_TYPE_KEY, integrationType)
                .put(USER_INTERFACE_ORIENTATION_KEY, getUserOrientation(context));
    }

    private static JSONObject populateCachedMetadata(Context context) {
        JSONObject meta = new JSONObject();
        try {
            meta.put(PLATFORM_KEY, "Android")
                    .put(PLATFORM_VERSION_KEY, Integer.toString(VERSION.SDK_INT))
                    .put(SDK_VERSION_KEY, BuildConfig.VERSION_NAME)
                    .put(MERCHANT_APP_ID_KEY, context.getPackageName())
                    .put(MERCHANT_APP_NAME_KEY,
                            getAppName(getApplicationInfo(context), context.getPackageManager()))
                    .put(MERCHANT_APP_VERSION_KEY, getAppVersion(context))
                    .put(DEVICE_ROOTED_KEY, isDeviceRooted())
                    .put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER)
                    .put(DEVICE_MODEL_KEY, Build.MODEL)
                    .put(ANDROID_ID_KEY, Secure.getString(context.getContentResolver(), Secure.ANDROID_ID))
                    .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY, getUUID(context))
                    .put(IS_SIMULATOR_KEY, detectEmulator());
        } catch (JSONException ignored) {}

        return meta;
    }

    private static ApplicationInfo getApplicationInfo(Context context) {
        ApplicationInfo applicationInfo;
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            applicationInfo = null;
        }

        return applicationInfo;
    }

    private static String getAppName(ApplicationInfo applicationInfo,
            PackageManager packageManager) {
        if (applicationInfo != null) {
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } else {
            return "ApplicationNameUnknown";
        }
    }

    private static String getAppVersion(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return "VersionUnknown";
        }
    }

    private static String isDeviceRooted() {
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

    private static String getNetworkType(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo().getTypeName();
    }

    private static String getUUID(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);

        String uuid = prefs.getString(BRAINTREE_UUID_KEY, null);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString().replace("-", "");
            prefs.edit().putString(BRAINTREE_UUID_KEY, uuid).apply();
        }

        return uuid;
    }

    private static String detectEmulator() {
        if ("google_sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "sdk".equalsIgnoreCase(Build.PRODUCT) ||
                "Genymotion".equalsIgnoreCase(Build.MANUFACTURER) ||
                Build.FINGERPRINT.contains("generic")) {
            return "true";
        } else {
            return "false";
        }
    }

    private static String getUserOrientation(Context context) {
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

    private static class AnalyticsRequest {

        private final String mIntegrationType;
        private final String mEvent;
        private final long mTimestamp;

        public AnalyticsRequest(String integration, String event){
            mIntegrationType = integration;
            mEvent = event;
            mTimestamp = System.currentTimeMillis() / 1000;
        }

        public String getEvent() {
            return "android." + mIntegrationType + "." + mEvent;
        }

        public long getTimestamp() {
            return mTimestamp;
        }

        public String getIntegrationType() { return mIntegrationType; }
    }
}
