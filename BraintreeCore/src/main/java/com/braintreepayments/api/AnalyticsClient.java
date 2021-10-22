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
    static final String ANALYTICS_INPUT_DATA_METADATA = "metadata";

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
        DeviceMetadata deviceMetadata = deviceInspector.getDeviceMetadata(context);

        JSONObject metadata = new JSONObject();
        try {
            metadata
                .put(SESSION_ID_KEY, sessionId)
                .put(INTEGRATION_TYPE_KEY, integration)
                .put(DEVICE_NETWORK_TYPE_KEY, deviceMetadata.getNetworkType())
                .put(USER_INTERFACE_ORIENTATION_KEY, deviceMetadata.getUserOrientation())
                .put(MERCHANT_APP_VERSION_KEY, deviceMetadata.getAppVersion())
                .put(PAYPAL_INSTALLED_KEY, deviceMetadata.isPayPalInstalled())
                .put(VENMO_INSTALLED_KEY, deviceMetadata.isVenmoInstalled())
                .put(DROP_IN_VERSION_KEY, deviceMetadata.getDropInVersion());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String metadataJSON = metadata.toString();

        sendEvent2(context, configuration, fullEventName, metadataJSON, timestamp);
    }

    void sendEvent2(Context context, Configuration configuration, String eventName, String metadata, long timestamp) {
        sendEventAndReturnId2(context, configuration, eventName, timestamp, metadata);
    }

    @VisibleForTesting
    UUID sendEventAndReturnId2(Context context, Configuration configuration, String eventName, long timestamp, String metadata) {
        lastKnownAnalyticsUrl = configuration.getAnalyticsUrl();

        scheduleAnalyticsWrite(context, eventName, timestamp, metadata);
        return scheduleAnalyticsUpload(context, configuration, httpClient.getAuthorization());
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
        OneTimeWorkRequest analyticsWorkRequest = createAnalyticsUploadRequest(configuration, authorization);

        WorkManager
                .getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        ANALYTICS_UPLOAD_WORK_NAME, ExistingWorkPolicy.KEEP, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    private UUID scheduleAnalyticsWrite(Context context, String eventName, long timestamp, String metadata) {
        OneTimeWorkRequest analyticsWorkRequest = createAnalyticsWriteRequest(eventName, timestamp, metadata);

        WorkManager
                .getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        ANALYTICS_WRITE_WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    static OneTimeWorkRequest createAnalyticsWriteRequest(String eventName, long timestamp, String metadata) {
        Data inputData = new Data.Builder()
                .putString(ANALYTICS_INPUT_DATA_EVENT_NAME, eventName)
                .putString(ANALYTICS_INPUT_DATA_METADATA, metadata)
                .putLong(ANALYTICS_INPUT_DATA_TIMESTAMP, timestamp)
                .build();

        return new OneTimeWorkRequest.Builder(AnalyticsWriteWorker.class)
                .setInputData(inputData)
                .build();
    }

    @VisibleForTesting
    static OneTimeWorkRequest createAnalyticsUploadRequest(Configuration configuration, Authorization authorization) {
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

        Context applicationContext = context.getApplicationContext();
        AnalyticsDatabase2 db = AnalyticsDatabase2.getDatabase(applicationContext);

        AnalyticsEventDao analyticsEventDao = db.analyticsEventDao();
        List<AnalyticsEvent2> events = analyticsEventDao.getAllEvents();

        JSONObject analyticsRequest = serializeEvents(context, httpClient.getAuthorization(), events);
        httpClient.post(analyticsUrl, analyticsRequest.toString(), configuration);
        analyticsEventDao.deleteEvents(events);
    }

    String getLastKnownAnalyticsUrl() {
        return lastKnownAnalyticsUrl;
    }

    private JSONObject serializeEvents(Context context, Authorization authorization,
                                       List<AnalyticsEvent2> events) throws JSONException {
        AnalyticsEvent2 primeEvent = events.get(0);

        JSONObject requestObject = new JSONObject();
        if (authorization instanceof ClientToken) {
            requestObject.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            requestObject.put(TOKENIZATION_KEY, authorization.getBearer());
        }

        JSONObject meta = new JSONObject(primeEvent.getMetadataJSON());
        meta
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
