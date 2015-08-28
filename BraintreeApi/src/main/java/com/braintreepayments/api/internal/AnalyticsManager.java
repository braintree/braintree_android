package com.braintreepayments.api.internal;

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
import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.ClientKey;
import com.braintreepayments.api.models.ClientToken;

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
public class AnalyticsManager {

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

    private static Context sContext;
    private static AnalyticsConfiguration sAnalyticConfiguration;
    private static JSONObject sCachedMetadata;

    @VisibleForTesting
    protected static BraintreeHttpClient sHttpClient;

    private static ArrayList<AnalyticsRequest> sRequestQueue;

    private AnalyticsManager() {}

    /**
     * Setup {@link AnalyticsManager}.
     *
     * @param context
     * @param analyticsConfiguration {@link AnalyticsConfiguration}
     * @param clientToken {@link ClientToken}
     */
    public static void setup(Context context, AnalyticsConfiguration analyticsConfiguration,
            ClientToken clientToken) {
        setup(context, analyticsConfiguration);
        sHttpClient = new BraintreeHttpClient(clientToken);
    }

    /**
     * Setup {@link AnalyticsManager}.
     *
     * @param context
     * @param analyticsConfiguration {@link AnalyticsConfiguration}
     * @param clientKey {@link ClientKey}
     */
    public static void setup(Context context, AnalyticsConfiguration analyticsConfiguration,
            ClientKey clientKey) {
        setup(context, analyticsConfiguration);
        sHttpClient = new BraintreeHttpClient(clientKey);
    }

    private static void setup(Context context, AnalyticsConfiguration analyticsConfiguration) {
        sContext = context;
        sAnalyticConfiguration = analyticsConfiguration;
        sRequestQueue = new ArrayList<>();
        sCachedMetadata = populateCachedMetadata();
    }

    /**
     * Queue or send analytics request with integration type and event.
     *
     * @param integrationType The current method of integration used.
     * @param eventFragment The analytics event to record.
     */
    public static void sendRequest(String integrationType, String eventFragment) {
        if (sHttpClient == null || sAnalyticConfiguration == null || !sAnalyticConfiguration.isEnabled()) {
            return;
        }

        AnalyticsRequest request = new AnalyticsRequest(integrationType, eventFragment);
        sRequestQueue.add(request);

        if (sRequestQueue.size() >= REQUEST_THRESHOLD) {
            processRequests();
            sRequestQueue.clear();
        }
    }

    /**
     * Batch and send remaining analytics events even if batch size has not been reached.
     */
    public static void flushEvents() {
        if (sHttpClient != null && sAnalyticConfiguration != null && sAnalyticConfiguration.isEnabled()) {
            processRequests();
        }
    }

    private static void processRequests() {
        if (sRequestQueue.size() == 0) {
            return;
        }

        ArrayList<AnalyticsRequest> requests = new ArrayList<>();
        requests.addAll(sRequestQueue);

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

            JSONObject fullMetaData = generateRequestBody(requests.get(0).getIntegrationType());

            String requestBody = new JSONObject()
                    .put(ANALYTICS_KEY, events)
                    .put(META_KEY, fullMetaData)
                    .toString();

            sHttpClient.post(sAnalyticConfiguration.getUrl(), requestBody, null);
        } catch (JSONException ignored) {}
    }

    private static JSONObject generateRequestBody(String integrationType) throws JSONException {
        return new JSONObject(sCachedMetadata.toString())
                .put(DEVICE_NETWORK_TYPE_KEY, getNetworkType())
                .put(INTEGRATION_TYPE_KEY, integrationType)
                .put(USER_INTERFACE_ORIENTATION_KEY, getUserOrientation());
    }

    private static JSONObject populateCachedMetadata() {
        JSONObject meta = new JSONObject();
        try {
            meta.put(PLATFORM_KEY, "Android")
                    .put(PLATFORM_VERSION_KEY, Integer.toString(VERSION.SDK_INT))
                    .put(SDK_VERSION_KEY, BuildConfig.VERSION_NAME)
                    .put(MERCHANT_APP_ID_KEY, sContext.getPackageName())
                    .put(MERCHANT_APP_NAME_KEY, getAppName(getApplicationInfo(), sContext.getPackageManager()))
                    .put(MERCHANT_APP_VERSION_KEY, getAppVersion())
                    .put(DEVICE_ROOTED_KEY, isDeviceRooted())
                    .put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER)
                    .put(DEVICE_MODEL_KEY, Build.MODEL)
                    .put(ANDROID_ID_KEY, Secure.getString(sContext.getContentResolver(), Secure.ANDROID_ID))
                    .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY, getUUID())
                    .put(IS_SIMULATOR_KEY, detectEmulator());
        } catch (JSONException ignored) {}

        return meta;
    }

    private static ApplicationInfo getApplicationInfo() {
        ApplicationInfo applicationInfo;
        String packageName = sContext.getPackageName();
        PackageManager packageManager = sContext.getPackageManager();
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

    private static String getAppVersion() {
        try {
            return sContext.getPackageManager()
                    .getPackageInfo(sContext.getPackageName(), 0).versionName;
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

    private static String getNetworkType() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) sContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo().getTypeName();
    }

    private static String getUUID() {
        SharedPreferences prefs =
                sContext.getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE);

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

    private static String getUserOrientation() {
        int orientation = sContext.getResources().getConfiguration().orientation;
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
            return mIntegrationType + ".android." + mEvent;
        }

        public long getTimestamp() {
            return mTimestamp;
        }

        public String getIntegrationType() { return mIntegrationType; }
    }
}
