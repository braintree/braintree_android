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

    static final String ANALYTICS_WRITE_WORK_NAME = "writeAnalytics";
    static final String ANALYTICS_INPUT_DATA_EVENT_NAME = "eventName";
    static final String ANALYTICS_INPUT_DATA_TIMESTAMP = "timestamp";

    static final String ANALYTICS_INPUT_DATA_SESSION_ID = "sessionId";
    static final String ANALYTICS_INPUT_DATA_INTEGRATION = "integration";

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

    void sendEvent2(Context context, Configuration configuration, String eventName, String sessionId, String integration) {
        String fullEventName = String.format("android.%s", eventName);
        long timestamp = System.currentTimeMillis();
        sendEvent2(context, configuration, fullEventName, timestamp, sessionId, integration);
    }

    void sendEvent2(Context context, Configuration configuration, String eventName, long timestamp, String sessionId, String integration) {
        sendEventAndReturnId2(context, configuration, eventName, timestamp, sessionId, integration);
    }

    @VisibleForTesting
    UUID sendEventAndReturnId2(Context context, Configuration configuration, String eventName, long timestamp, String sessionId, String integration) {
        lastKnownAnalyticsUrl = configuration.getAnalyticsUrl();

        scheduleAnalyticsWrite(context, eventName, timestamp);
        return scheduleAnalyticsUpload(context, configuration, httpClient.getAuthorization(), sessionId, integration);
    }

    private UUID scheduleAnalyticsUpload(Context context, Configuration configuration, Authorization authorization, String sessionId, String integration) {
        OneTimeWorkRequest analyticsWorkRequest = createAnalyticsUploadRequest(configuration, authorization, sessionId, integration);

        WorkManager
                .getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        ANALYTICS_UPLOAD_WORK_NAME, ExistingWorkPolicy.KEEP, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    private UUID scheduleAnalyticsWrite(Context context, String eventName, long timestamp) {
        OneTimeWorkRequest analyticsWorkRequest = createAnalyticsWriteRequest(eventName, timestamp);

        WorkManager
                .getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        ANALYTICS_WRITE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    static OneTimeWorkRequest createAnalyticsWriteRequest(String eventName, long timestamp) {
        Data inputData = new Data.Builder()
                .putString(ANALYTICS_INPUT_DATA_EVENT_NAME, eventName)
                .putLong(ANALYTICS_INPUT_DATA_TIMESTAMP, timestamp)
                .build();

        return new OneTimeWorkRequest.Builder(AnalyticsWriteWorker.class)
                .setInputData(inputData)
                .build();
    }

    @VisibleForTesting
    static OneTimeWorkRequest createAnalyticsUploadRequest(Configuration configuration, Authorization authorization, String sessionId, String integration) {
        Data inputData = new Data.Builder()
                .putString(ANALYTICS_INPUT_DATA_AUTHORIZATION_KEY, authorization.toString())
                .putString(ANALYTICS_INPUT_DATA_CONFIGURATION_KEY, configuration.toJson())
                .putString(ANALYTICS_INPUT_DATA_SESSION_ID, sessionId)
                .putString(ANALYTICS_INPUT_DATA_INTEGRATION, integration)
                .build();

        return new OneTimeWorkRequest.Builder(AnalyticsUploadWorker.class)
                .setInitialDelay(30, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build();
    }

    void uploadAnalytics(Context context, Configuration configuration, String sessionId, String integration) throws Exception {
        String analyticsUrl = configuration.getAnalyticsUrl();

        Context applicationContext = context.getApplicationContext();
        AnalyticsDatabase2 db = AnalyticsDatabase2.getDatabase(applicationContext);

        AnalyticsEventDao analyticsEventDao = db.analyticsEventDao();
        List<AnalyticsEvent2> events = analyticsEventDao.getAllEvents();

        DeviceMetadata metadata = deviceInspector.getDeviceMetadata(context, sessionId, integration);

        JSONObject analyticsRequest = serializeEvents(context, httpClient.getAuthorization(), events, metadata);
        httpClient.post(analyticsUrl, analyticsRequest.toString(), configuration);
        analyticsEventDao.deleteEvents(events);
    }

    String getLastKnownAnalyticsUrl() {
        return lastKnownAnalyticsUrl;
    }

    private JSONObject serializeEvents(Context context, Authorization authorization, List<AnalyticsEvent2> events, DeviceMetadata metadata) throws JSONException {
        JSONObject requestObject = new JSONObject();
        if (authorization instanceof ClientToken) {
            requestObject.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            requestObject.put(TOKENIZATION_KEY, authorization.getBearer());
        }
        requestObject.put(META_KEY, metadata.toJSON());

        JSONArray eventObjects = new JSONArray();
        JSONObject eventObject;
        for (AnalyticsEvent2 analyticsEvent : events) {
            eventObject = new JSONObject()
                    .put(KIND_KEY, analyticsEvent.getEvent())
                    .put(TIMESTAMP_KEY, analyticsEvent.getTimestamp());

            eventObjects.put(eventObject);
        }
        requestObject.put(ANALYTICS_KEY, eventObjects);

        return requestObject;
    }
}
