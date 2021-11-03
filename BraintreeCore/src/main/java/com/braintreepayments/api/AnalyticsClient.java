package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
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

    static final String WORK_NAME_ANALYTICS_UPLOAD = "uploadAnalytics";
    static final String WORK_NAME_ANALYTICS_WRITE = "writeAnalytics";

    static final String WORK_INPUT_KEY_AUTHORIZATION = "authorization";
    static final String WORK_INPUT_KEY_CONFIGURATION = "configuration";
    static final String WORK_INPUT_KEY_EVENT_NAME = "eventName";
    static final String WORK_INPUT_KEY_INTEGRATION = "integration";
    static final String WORK_INPUT_KEY_SESSION_ID = "sessionId";
    static final String WORK_INPUT_KEY_TIMESTAMP = "timestamp";

    private final BraintreeHttpClient httpClient;
    private final DeviceInspector deviceInspector;
    private final AnalyticsDatabase analyticsDatabase;

    private String lastKnownAnalyticsUrl;

    AnalyticsClient(Context context, Authorization authorization) {
        this(new BraintreeHttpClient(authorization), new DeviceInspector(), AnalyticsDatabase.getDatabase(context));
    }

    @VisibleForTesting
    AnalyticsClient(BraintreeHttpClient httpClient, DeviceInspector deviceInspector, AnalyticsDatabase analyticsDatabase) {
        this.httpClient = httpClient;
        this.deviceInspector = deviceInspector;
        this.analyticsDatabase = analyticsDatabase;
    }

    void sendEvent(Context context, Configuration configuration, String eventName, String sessionId, String integration) {
        String fullEventName = String.format("android.%s", eventName);
        long timestamp = System.currentTimeMillis();
        sendEvent(context, configuration, fullEventName, timestamp, sessionId, integration);
    }

    void sendEvent(Context context, Configuration configuration, String eventName, long timestamp, String sessionId, String integration) {
        sendEventAndReturnId(context, configuration, eventName, timestamp, sessionId, integration);
    }

    @VisibleForTesting
    UUID sendEventAndReturnId(Context context, Configuration configuration, String eventName, long timestamp, String sessionId, String integration) {
        lastKnownAnalyticsUrl = configuration.getAnalyticsUrl();

        scheduleAnalyticsWrite(context, eventName, timestamp);
        return scheduleAnalyticsUpload(context, configuration, httpClient.getAuthorization(), sessionId, integration);
    }

    private UUID scheduleAnalyticsUpload(Context context, Configuration configuration, Authorization authorization, String sessionId, String integration) {
        OneTimeWorkRequest analyticsWorkRequest = createAnalyticsUploadRequest(configuration, authorization, sessionId, integration);

        WorkManager
                .getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        WORK_NAME_ANALYTICS_UPLOAD, ExistingWorkPolicy.KEEP, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    private UUID scheduleAnalyticsWrite(Context context, String eventName, long timestamp) {
        Authorization authorization = httpClient.getAuthorization();
        OneTimeWorkRequest analyticsWorkRequest = createAnalyticsWriteRequest(authorization, eventName, timestamp);

        WorkManager
                .getInstance(context.getApplicationContext())
                .enqueueUniqueWork(
                        WORK_NAME_ANALYTICS_WRITE, ExistingWorkPolicy.APPEND_OR_REPLACE, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    static OneTimeWorkRequest createAnalyticsWriteRequest(Authorization authorization, String eventName, long timestamp) {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
                .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
                .build();

        return new OneTimeWorkRequest.Builder(AnalyticsWriteToDbWorker.class)
                .setInputData(inputData)
                .build();
    }

    @VisibleForTesting
    static OneTimeWorkRequest createAnalyticsUploadRequest(Configuration configuration, Authorization authorization, String sessionId, String integration) {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        return new OneTimeWorkRequest.Builder(AnalyticsUploadFromDbWorker.class)
                .setInitialDelay(30, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build();
    }

    ListenableWorker.Result writeAnalytics(Context context, Data inputData) {
        String eventName = inputData.getString(WORK_INPUT_KEY_EVENT_NAME);
        long timestamp = inputData.getLong(WORK_INPUT_KEY_TIMESTAMP, 0);
        AnalyticsEvent event = new AnalyticsEvent(eventName, timestamp);

        AnalyticsEventDao analyticsEventDao = analyticsDatabase.analyticsEventDao();
        analyticsEventDao.insertEvent(event);

        return ListenableWorker.Result.success();
    }

    ListenableWorker.Result uploadAnalytics(Context context, Data inputData) {
        Configuration configuration = getConfigurationFromData(inputData);
        String sessionId = inputData.getString(WORK_INPUT_KEY_SESSION_ID);
        String integration = inputData.getString(WORK_INPUT_KEY_INTEGRATION);

        if (configuration == null || sessionId == null || integration == null) {
            return ListenableWorker.Result.failure();
        }

        try {
            uploadAnalytics(context, configuration, sessionId, integration);
            return ListenableWorker.Result.success();
        } catch (Exception e) {
            return ListenableWorker.Result.failure();
        }
    }

    void uploadAnalytics(Context context, Configuration configuration, String sessionId, String integration) throws Exception {
        String analyticsUrl = configuration.getAnalyticsUrl();

        AnalyticsEventDao analyticsEventDao = analyticsDatabase.analyticsEventDao();
        List<AnalyticsEvent> events = analyticsEventDao.getAllEvents();

        boolean shouldUploadAnalytics = !events.isEmpty();
        if (shouldUploadAnalytics) {
            DeviceMetadata metadata = deviceInspector.getDeviceMetadata(context, sessionId, integration);
            JSONObject analyticsRequest = serializeEvents(context, httpClient.getAuthorization(), events, metadata);
            httpClient.post(analyticsUrl, analyticsRequest.toString(), configuration);
            analyticsEventDao.deleteEvents(events);
        }
    }

    String getLastKnownAnalyticsUrl() {
        return lastKnownAnalyticsUrl;
    }

    private JSONObject serializeEvents(Context context, Authorization authorization, List<AnalyticsEvent> events, DeviceMetadata metadata) throws JSONException {
        JSONObject requestObject = new JSONObject();
        if (authorization instanceof ClientToken) {
            requestObject.put(AUTHORIZATION_FINGERPRINT_KEY, authorization.getBearer());
        } else {
            requestObject.put(TOKENIZATION_KEY, authorization.getBearer());
        }
        requestObject.put(META_KEY, metadata.toJSON());

        JSONArray eventObjects = new JSONArray();
        JSONObject eventObject;
        for (AnalyticsEvent analyticsEvent : events) {
            eventObject = new JSONObject()
                    .put(KIND_KEY, analyticsEvent.getName())
                    .put(TIMESTAMP_KEY, analyticsEvent.getTimestamp());

            eventObjects.put(eventObject);
        }
        requestObject.put(ANALYTICS_KEY, eventObjects);

        return requestObject;
    }

    private static Configuration getConfigurationFromData(Data inputData) {
        if (inputData != null) {
            String configJson = inputData.getString(WORK_INPUT_KEY_CONFIGURATION);
            if (configJson != null) {
                try {
                    return Configuration.fromJson(configJson);
                } catch (JSONException e) { /* ignored */ }
            }
        }
        return null;
    }
}
