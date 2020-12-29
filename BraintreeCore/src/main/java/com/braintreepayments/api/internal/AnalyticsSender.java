package com.braintreepayments.api.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

public class AnalyticsSender {

    private static final String ANALYTICS_KEY = "analytics";
    private static final String KIND_KEY = "kind";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String META_KEY = "_meta";
    private static final String TOKENIZATION_KEY = "tokenization_key";
    private static final String AUTHORIZATION_FINGERPRINT_KEY = "authorization_fingerprint";
    private static final String PLATFORM_KEY = "platform";
    private static final String PLATFORM_VERSION_KEY = "platformVersion";
    private static final String SDK_VERSION_KEY = "sdkVersion";
    private static final String MERCHANT_APP_ID_KEY = "merchantAppId";
    private static final String MERCHANT_APP_NAME_KEY = "merchantAppName";
    private static final String DEVICE_ROOTED_KEY = "deviceRooted";
    private static final String DEVICE_MANUFACTURER_KEY = "deviceManufacturer";
    private static final String DEVICE_MODEL_KEY = "deviceModel";
    private static final String DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY = "deviceAppGeneratedPersistentUuid";
    private static final String IS_SIMULATOR_KEY = "isSimulator";

    public static void send(Context context, Authorization authorization, Configuration configuration, BraintreeHttpClient httpClient,
                            String analyticsUrl, boolean synchronous) {
        final AnalyticsDatabase db = AnalyticsDatabase.getInstance(context);

        List<List<AnalyticsEvent>> events = db.getPendingRequests();

        try {
            JSONObject analyticsRequest;
            for (final List<AnalyticsEvent> innerEvents : events) {
                analyticsRequest = serializeEvents(context, authorization, innerEvents);
                try {
                    if (synchronous) {
                        httpClient.post(analyticsUrl, analyticsRequest.toString(), configuration);
                        db.removeEvents(innerEvents);
                    } else {
                        httpClient.post(analyticsUrl, analyticsRequest.toString(), configuration, new HttpResponseCallback() {
                            @Override
                            public void success(String responseBody) {
                                db.removeEvents(innerEvents);
                            }

                            @Override
                            public void failure(Exception exception) {}
                        });
                    }
                } catch (Exception ignored) {}
            }
        } catch (JSONException ignored) {}
    }

    private static JSONObject serializeEvents(Context context, Authorization authorization,
            List<AnalyticsEvent> events) throws JSONException {
        AnalyticsEvent primeEvent = events.get(0);

        JSONObject requestObject = new JSONObject();
        if (authorization instanceof ClientToken) {
            requestObject.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            requestObject.put(TOKENIZATION_KEY, authorization.getBearer());
        }

        JSONObject meta = primeEvent.metadata
                .put(PLATFORM_KEY, "Android")
                .put(PLATFORM_VERSION_KEY, Integer.toString(VERSION.SDK_INT))
                .put(SDK_VERSION_KEY, BuildConfig.VERSION_NAME)
                .put(MERCHANT_APP_ID_KEY, context.getPackageName())
                .put(MERCHANT_APP_NAME_KEY, getAppName(context))
                .put(DEVICE_ROOTED_KEY, isDeviceRooted())
                .put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER)
                .put(DEVICE_MODEL_KEY, Build.MODEL)
                .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY,
                        UUIDHelper.getPersistentUUID(context))
                .put(IS_SIMULATOR_KEY, detectEmulator());
        requestObject.put(META_KEY, meta);

        JSONArray eventObjects = new JSONArray();
        JSONObject eventObject;
        for (AnalyticsEvent analyticsEvent : events) {
            eventObject = new JSONObject()
                    .put(KIND_KEY, analyticsEvent.event)
                    .put(TIMESTAMP_KEY, analyticsEvent.timestamp);

            eventObjects.put(eventObject);
        }
        requestObject.put(ANALYTICS_KEY, eventObjects);

        return requestObject;
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

    private static String getAppName(Context context) {
        ApplicationInfo applicationInfo;
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
        } catch (NameNotFoundException e) {
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

    private static String isDeviceRooted() {
        String buildTags = android.os.Build.TAGS;
        boolean check1 = buildTags != null && buildTags.contains("test-keys");

        boolean check2;
        try {
            check2 = new File("/system/app/Superuser.apk").exists();
        } catch (Exception e) {
            check2 = false;
        }

        boolean check3;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            check3 = in.readLine() != null;
        } catch (Exception e) {
            check3 = false;
        }

        return Boolean.toString(check1 || check2 || check3);
    }
}
