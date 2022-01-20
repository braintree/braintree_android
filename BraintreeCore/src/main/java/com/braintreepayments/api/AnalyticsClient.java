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

import java.util.Arrays;
import java.util.Collections;
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

    private static final long INVALID_TIMESTAMP = -1;

    static final String WORK_NAME_ANALYTICS_UPLOAD = "uploadAnalytics";
    static final String WORK_NAME_ANALYTICS_WRITE = "writeAnalyticsToDb";

    static final String WORK_INPUT_KEY_AUTHORIZATION = "authorization";
    static final String WORK_INPUT_KEY_CONFIGURATION = "configuration";
    static final String WORK_INPUT_KEY_EVENT_NAME = "eventName";
    static final String WORK_INPUT_KEY_INTEGRATION = "integration";
    static final String WORK_INPUT_KEY_SESSION_ID = "sessionId";
    static final String WORK_INPUT_KEY_TIMESTAMP = "timestamp";

    private final BraintreeHttpClient httpClient;
    private final DeviceInspector deviceInspector;
    private final AnalyticsDatabase analyticsDatabase;
    private final WorkManager workManager;

    private String lastKnownAnalyticsUrl;

    AnalyticsClient(Context context) {
        this(
                new BraintreeHttpClient(),
                AnalyticsDatabase.getInstance(context.getApplicationContext()),
                WorkManager.getInstance(context.getApplicationContext()),
                new DeviceInspector()
        );
    }

    @VisibleForTesting
    AnalyticsClient(BraintreeHttpClient httpClient, AnalyticsDatabase analyticsDatabase, WorkManager workManager, DeviceInspector deviceInspector) {
        this.httpClient = httpClient;
        this.workManager = workManager;
        this.deviceInspector = deviceInspector;
        this.analyticsDatabase = analyticsDatabase;
    }

    void sendEvent(Configuration configuration, String eventName, String sessionId, String integration, Authorization authorization) {
        long timestamp = System.currentTimeMillis();
        sendEvent(configuration, eventName, sessionId, integration, timestamp, authorization);
    }

    @VisibleForTesting
    UUID sendEvent(Configuration configuration, String eventName, String sessionId, String integration, long timestamp, Authorization authorization) {
        lastKnownAnalyticsUrl = configuration.getAnalyticsUrl();

        String fullEventName = String.format("android.%s", eventName);
        scheduleAnalyticsWrite(fullEventName, timestamp, authorization);
        return scheduleAnalyticsUpload(configuration, authorization, sessionId, integration);
    }

    private void scheduleAnalyticsWrite(String eventName, long timestamp, Authorization authorization) {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
                .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
                .build();

        OneTimeWorkRequest analyticsWorkRequest =
                new OneTimeWorkRequest.Builder(AnalyticsWriteToDbWorker.class)
                        .setInputData(inputData)
                        .build();
        workManager.enqueueUniqueWork(
                WORK_NAME_ANALYTICS_WRITE, ExistingWorkPolicy.APPEND_OR_REPLACE, analyticsWorkRequest);
    }

    ListenableWorker.Result writeAnalytics(Data inputData) {
        String eventName = inputData.getString(WORK_INPUT_KEY_EVENT_NAME);
        long timestamp = inputData.getLong(WORK_INPUT_KEY_TIMESTAMP, INVALID_TIMESTAMP);

        ListenableWorker.Result result;
        if (eventName == null || timestamp == INVALID_TIMESTAMP) {
            result = ListenableWorker.Result.failure();
        } else {
            AnalyticsEvent event = new AnalyticsEvent(eventName, timestamp);
            AnalyticsEventDao analyticsEventDao = analyticsDatabase.analyticsEventDao();
            analyticsEventDao.insertEvent(event);

            result = ListenableWorker.Result.success();
        }
        return result;
    }

    private UUID scheduleAnalyticsUpload(Configuration configuration, Authorization authorization, String sessionId, String integration) {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        OneTimeWorkRequest analyticsWorkRequest =
                new OneTimeWorkRequest.Builder(AnalyticsUploadWorker.class)
                        .setInitialDelay(30, TimeUnit.SECONDS)
                        .setInputData(inputData)
                        .build();
        workManager.enqueueUniqueWork(
                WORK_NAME_ANALYTICS_UPLOAD, ExistingWorkPolicy.KEEP, analyticsWorkRequest);
        return analyticsWorkRequest.getId();
    }

    ListenableWorker.Result uploadAnalytics(Context context, Data inputData) {
        Configuration configuration = getConfigurationFromData(inputData);
        Authorization authorization = getAuthorizationFromData(inputData);

        String sessionId = inputData.getString(WORK_INPUT_KEY_SESSION_ID);
        String integration = inputData.getString(WORK_INPUT_KEY_INTEGRATION);

        boolean shouldFail =
            Arrays.asList(configuration, authorization, sessionId, integration).contains(null);
        if (shouldFail) {
            return ListenableWorker.Result.failure();
        }

        try {
            AnalyticsEventDao analyticsEventDao = analyticsDatabase.analyticsEventDao();
            List<AnalyticsEvent> events = analyticsEventDao.getAllEvents();

            boolean shouldUploadAnalytics = !events.isEmpty();
            if (shouldUploadAnalytics) {
                DeviceMetadata metadata = deviceInspector.getDeviceMetadata(context, sessionId, integration);
                JSONObject analyticsRequest = serializeEvents(authorization, events, metadata);

                String analyticsUrl = configuration.getAnalyticsUrl();
                httpClient.post(analyticsUrl, analyticsRequest.toString(), configuration, authorization);
                analyticsEventDao.deleteEvents(events);
            }
            return ListenableWorker.Result.success();
        } catch (Exception e) {
            return ListenableWorker.Result.failure();
        }
    }

    void reportCrash(Context context, String sessionId, String integration, Authorization authorization) {
        reportCrash(context, sessionId, integration, System.currentTimeMillis(), authorization);
    }

    @VisibleForTesting
    void reportCrash(Context context, String sessionId, String integration, long timestamp, Authorization authorization) {
        if (lastKnownAnalyticsUrl == null || authorization == null) {
            return;
        }

        DeviceMetadata metadata = deviceInspector.getDeviceMetadata(context, sessionId, integration);
        AnalyticsEvent event = new AnalyticsEvent("android.crash", timestamp);
        List<AnalyticsEvent> events = Collections.singletonList(event);
        try {
            JSONObject analyticsRequest = serializeEvents(authorization, events, metadata);
            httpClient.post(lastKnownAnalyticsUrl, analyticsRequest.toString(), null, authorization, new HttpNoResponse());
        } catch (JSONException e) { /* ignored */ }
    }

    private JSONObject serializeEvents(Authorization authorization, List<AnalyticsEvent> events, DeviceMetadata metadata) throws JSONException {
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

    private static Authorization getAuthorizationFromData(Data inputData) {
        if (inputData != null) {
            String authString = inputData.getString(WORK_INPUT_KEY_AUTHORIZATION);
            if (authString != null) {
                return Authorization.fromString(authString);
            }
        }
        return null;
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
