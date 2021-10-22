package com.braintreepayments.api;

import android.content.Context;
import android.os.Build;

import androidx.annotation.VisibleForTesting;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class AnalyticsClient {

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

    private static final String SESSION_ID_KEY = "sessionId";
    private static final String DEVICE_NETWORK_TYPE_KEY = "deviceNetworkType";
    private static final String USER_INTERFACE_ORIENTATION_KEY = "userInterfaceOrientation";
    private static final String MERCHANT_APP_VERSION_KEY = "merchantAppVersion";
    private static final String PAYPAL_INSTALLED_KEY = "paypalInstalled";
    private static final String VENMO_INSTALLED_KEY = "venmoInstalled";
    private static final String INTEGRATION_TYPE_KEY = "integrationType";
    private static final String DROP_IN_VERSION_KEY = "dropinVersion";

    static final String ANALYTICS_UPLOAD_WORK_NAME = "uploadAnalytics";
    static final String ANALYTICS_INPUT_DATA_CONFIGURATION_KEY = "configuration";
    static final String ANALYTICS_INPUT_DATA_AUTHORIZATION_KEY = "authorization";

    private final BraintreeHttpClient httpClient;
    private final DeviceInspector deviceInspector;
    private final UUIDHelper uuidHelper;
    private String lastKnownAnalyticsUrl;

    AnalyticsClient(Authorization authorization) {
        this(new BraintreeHttpClient(authorization), new DeviceInspector(), new UUIDHelper());
    }

    @VisibleForTesting
    AnalyticsClient(BraintreeHttpClient httpClient, DeviceInspector deviceInspector, UUIDHelper uuidHelper) {
        this.httpClient = httpClient;
        this.deviceInspector = deviceInspector;
        this.uuidHelper = uuidHelper;
    }

    void sendEvent(Context context, Configuration configuration, String eventName, String sessionId, String integration) {
        String fullEventName = String.format("android.%s", eventName);
        long timestamp = System.currentTimeMillis();
        DeviceMetadata deviceMetadata = deviceInspector.getDeviceMetadata(context);
        
    }

    void sendEvent(Context context, Configuration configuration, AnalyticsEvent event) {
        sendEventAndReturnId(context, configuration, event);
    }

    @VisibleForTesting
    UUID sendEventAndReturnId(Context context, Configuration configuration, AnalyticsEvent event) {
        lastKnownAnalyticsUrl = configuration.getAnalyticsUrl();

        Context applicationContext = context.getApplicationContext();
        AnalyticsDatabase db = AnalyticsDatabase.getInstance(applicationContext);
        db.addEvent(event);

        return scheduleAnalyticsUpload(context, configuration, httpClient.getAuthorization());
    }

    private UUID scheduleAnalyticsUpload(Context context, Configuration configuration, Authorization authorization) {
        OneTimeWorkRequest analyticsWorkRequest = createAnalyticsWorkerRequest(configuration, authorization);

        WorkManager
                .getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        ANALYTICS_UPLOAD_WORK_NAME, ExistingWorkPolicy.KEEP, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    @VisibleForTesting
    static OneTimeWorkRequest createAnalyticsWorkerRequest(Configuration configuration, Authorization authorization) {
        Data inputData = new Data.Builder()
                .putString(ANALYTICS_INPUT_DATA_AUTHORIZATION_KEY, authorization.toString())
                .putString(ANALYTICS_INPUT_DATA_CONFIGURATION_KEY, configuration.toJson())
                .build();

        return new OneTimeWorkRequest.Builder(AnalyticsUploadWorker.class)
                .setInitialDelay(30, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build();
    }

    void uploadAnalytics(Context context, Configuration configuration) throws Exception {
        String analyticsUrl = configuration.getAnalyticsUrl();

        final AnalyticsDatabase db = AnalyticsDatabase.getInstance(context);
        List<List<AnalyticsEvent>> events = db.getPendingRequests();

        try {
            for (final List<AnalyticsEvent> innerEvents : events) {
                JSONObject analyticsRequest = serializeEvents(context, httpClient.getAuthorization(), innerEvents);
                httpClient.post(analyticsUrl, analyticsRequest.toString(), configuration);
                db.removeEvents(innerEvents);
            }
        } catch (JSONException ignored) {}
    }

    String getLastKnownAnalyticsUrl() {
        return lastKnownAnalyticsUrl;
    }

    private JSONObject serializeEvents(Context context, Authorization authorization,
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
                .put(PLATFORM_VERSION_KEY, Integer.toString(Build.VERSION.SDK_INT))
                .put(SDK_VERSION_KEY, BuildConfig.VERSION_NAME)
                .put(MERCHANT_APP_ID_KEY, context.getPackageName())
                .put(MERCHANT_APP_NAME_KEY, deviceInspector.getAppName(context))
                .put(DEVICE_ROOTED_KEY, deviceInspector.isDeviceRooted())
                .put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER)
                .put(DEVICE_MODEL_KEY, Build.MODEL)
                .put(DEVICE_APP_GENERATED_PERSISTENT_UUID_KEY,
                        uuidHelper.getPersistentUUID(context))
                .put(IS_SIMULATOR_KEY, deviceInspector.isDeviceEmulator());
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
}
