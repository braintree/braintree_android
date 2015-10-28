package com.braintreepayments.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;

import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Centralized location for caching, queuing, and sending Analytics events
 */
class AnalyticsManager {

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

    private static final int REQUEST_THRESHOLD = 5;

    private static JSONObject sCachedMetadata;

    static ArrayList<AnalyticsRequest> sRequestQueue = new ArrayList<>();

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
            public void onConfigurationFetched(Configuration configuration) {
                if (!configuration.getAnalytics().isEnabled()) {
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
            public void onConfigurationFetched(Configuration configuration) {
                if (configuration.getAnalytics().isEnabled()) {
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
                JSONObject event = new JSONObject()
                        .put(KIND_KEY, request.getEvent())
                        .put(TIMESTAMP_KEY, request.getTimestamp());

                events.put(event);
            }

            JSONObject metadata = generateMetadata(fragment.getApplicationContext(),
                    requests.get(0).getIntegrationType());

            JSONObject fullRequest = new JSONObject()
                    .put(ANALYTICS_KEY, events)
                    .put(META_KEY, metadata);

            if (fragment.getAuthorization() instanceof ClientToken) {
                fullRequest.put(AUTHORIZATION_FINGERPRINT_KEY,
                        ((ClientToken) fragment.getAuthorization()).getAuthorizationFingerprint());
            } else {
                fullRequest.put(TOKENIZATION_KEY, fragment.getAuthorization().toString());
            }

            fragment.getHttpClient().post(fragment.getConfiguration().getAnalytics().getUrl(),
                    fullRequest.toString(), null);
        } catch (JSONException ignored) {}
    }

    private static JSONObject generateMetadata(Context context, String integrationType)
            throws JSONException {
        if (sCachedMetadata == null) {
            sCachedMetadata = populateCachedMetadata(context);
        }

        return new JSONObject(sCachedMetadata.toString())
                .put(DEVICE_NETWORK_TYPE_KEY, DeviceMetadata.getNetworkType(context))
                .put(INTEGRATION_TYPE_KEY, integrationType)
                .put(USER_INTERFACE_ORIENTATION_KEY, DeviceMetadata.getUserOrientation(context));
    }

    private static JSONObject populateCachedMetadata(Context context) {
        JSONObject meta = new JSONObject();
        try {
            meta.put(PLATFORM_KEY, "Android")
                    .put(PLATFORM_VERSION_KEY, Integer.toString(VERSION.SDK_INT))
                    .put(SDK_VERSION_KEY, BuildConfig.VERSION_NAME)
                    .put(MERCHANT_APP_ID_KEY, context.getPackageName())
                    .put(MERCHANT_APP_NAME_KEY,
                            DeviceMetadata.getAppName(getApplicationInfo(context),
                                    context.getPackageManager()))
                    .put(MERCHANT_APP_VERSION_KEY, DeviceMetadata.getAppVersion(context))
                    .put(DEVICE_ROOTED_KEY, DeviceMetadata.isDeviceRooted())
                    .put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER)
                    .put(DEVICE_MODEL_KEY, Build.MODEL)
                    .put(ANDROID_ID_KEY, Secure.getString(context.getContentResolver(), Secure.ANDROID_ID))
                    .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY, DeviceMetadata.getUUID(context))
                    .put(IS_SIMULATOR_KEY, DeviceMetadata.detectEmulator());
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

    static class AnalyticsRequest {

        private final String mIntegrationType;
        private final String mEvent;
        private final long mTimestamp;

        AnalyticsRequest(String integration, String event){
            mIntegrationType = integration;
            mEvent = event;
            mTimestamp = System.currentTimeMillis() / 1000;
        }

        String getEvent() {
            return "android." + mIntegrationType + "." + mEvent;
        }

        long getTimestamp() {
            return mTimestamp;
        }

        String getIntegrationType() { return mIntegrationType; }
    }
}
