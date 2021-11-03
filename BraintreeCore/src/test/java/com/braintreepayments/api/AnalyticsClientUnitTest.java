package com.braintreepayments.api;

import static com.braintreepayments.api.AnalyticsClient.WORK_INPUT_KEY_AUTHORIZATION;
import static com.braintreepayments.api.AnalyticsClient.WORK_INPUT_KEY_CONFIGURATION;
import static com.braintreepayments.api.AnalyticsClient.WORK_INPUT_KEY_EVENT_NAME;
import static com.braintreepayments.api.AnalyticsClient.WORK_INPUT_KEY_INTEGRATION;
import static com.braintreepayments.api.AnalyticsClient.WORK_INPUT_KEY_SESSION_ID;
import static com.braintreepayments.api.AnalyticsClient.WORK_INPUT_KEY_TIMESTAMP;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.impl.model.WorkSpec;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsClientUnitTest {

    private Context context;
    private Authorization authorization;

    private BraintreeHttpClient httpClient;
    private DeviceInspector deviceInspector;

    private String eventName;
    private long timestamp;
    private String sessionId;
    private String integration;

    AnalyticsDatabase analyticsDatabase;
    AnalyticsEventDao analyticsEventDao;

    @Before
    public void setup() throws InvalidArgumentException, GeneralSecurityException, IOException {
        timestamp = 123;
        eventName = "sample-event-name";
        sessionId = "sample-session-id";
        integration = "sample-integration";

        authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        context = ApplicationProvider.getApplicationContext();

        httpClient = mock(BraintreeHttpClient.class);
        deviceInspector = mock(DeviceInspector.class);

        analyticsDatabase = mock(AnalyticsDatabase.class);
        analyticsEventDao = mock(AnalyticsEventDao.class);
        when(analyticsDatabase.analyticsEventDao()).thenReturn(analyticsEventDao);

        WorkManagerTestInitHelper.initializeTestWorkManager(context);
    }

    @Test
    public void createAnalyticsUploadRequest_returnsAnalyticsUploadWorkerWithDelay() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        OneTimeWorkRequest result = AnalyticsClient.createAnalyticsUploadRequest(
                configuration, authorization, sessionId, integration);

        WorkSpec workSpec = result.getWorkSpec();
        assertEquals(30000, workSpec.initialDelay);
        assertEquals(AnalyticsUploadFromDbWorker.class.getName(), workSpec.workerClassName);

        assertEquals(configuration.toJson(), workSpec.input.getString("configuration"));
        assertEquals(authorization.toString(), workSpec.input.getString("authorization"));
        assertEquals("sample-session-id", workSpec.input.getString("sessionId"));
        assertEquals("sample-integration", workSpec.input.getString("integration"));
    }

    @Test
    public void createAnalyticsWriteRequest_returnsAnalyticsUploadWorkerWithDelay() {
        OneTimeWorkRequest result =
            AnalyticsClient.createAnalyticsWriteRequest(authorization, eventName, timestamp);

        WorkSpec workSpec = result.getWorkSpec();
        assertEquals(AnalyticsWriteToDbWorker.class.getName(), workSpec.workerClassName);

        assertEquals(authorization.toString(), workSpec.input.getString("authorization"));
        assertEquals("sample-event-name", workSpec.input.getString("eventName"));
        assertEquals(123, workSpec.input.getLong("timestamp", 0));
    }

    @Test
    public void writeAnalytics_whenEventNameAndTimestampArePresent_returnsSuccess() {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
                .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        ListenableWorker.Result result = sut.writeAnalytics(context, inputData);
        assertTrue(result instanceof ListenableWorker.Result.Success);
    }

    @Test
    public void writeAnalytics_addsEventToAnalyticsDatabaseAndReturnsSuccess() {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
                .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        sut.writeAnalytics(context, inputData);

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventDao).insertEvent(captor.capture());

        AnalyticsEvent event = captor.getValue();
        assertEquals("sample-event-name", event.getName());
        assertEquals(123, event.getTimestamp());
    }

    @Test
    public void sendEvent_setsLastKnownAnalyticsUrl() throws JSONException {
        when(httpClient.getAuthorization()).thenReturn(authorization);

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        sut.sendEvent(context, configuration, eventName, sessionId, integration);

        assertEquals("analytics_url", sut.getLastKnownAnalyticsUrl());
    }

    @Test
    public void sendEventAndReturnId_enqueuesAnalyticsWorker() throws ExecutionException, InterruptedException, JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        when(httpClient.getAuthorization()).thenReturn(authorization);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        UUID workSpecId = sut.sendEventAndReturnId(context, configuration, eventName, timestamp, sessionId, integration);

        WorkInfo analyticsWorkerInfo = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(WorkInfo.State.ENQUEUED, analyticsWorkerInfo.getState());
    }

    @Test
    public void uploadAnalytics_whenNoEventsExist_doesNothing() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        sut.uploadAnalytics(context, inputData);

        verifyZeroInteractions(httpClient);
    }

    @Test
    public void uploadAnalytics_whenEventsExist_sendsCorrectMetaData() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);
        when(httpClient.getAuthorization()).thenReturn(authorization);

        AnalyticsEvent event = new AnalyticsEvent(eventName, timestamp);
        when(analyticsEventDao.getAllEvents()).thenReturn(Collections.singletonList(event));

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        sut.uploadAnalytics(context, inputData);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(anyString(), captor.capture(), any(Configuration.class));
        JSONObject object = new JSONObject(captor.getValue());
        JSONObject meta = object.getJSONObject("_meta");

        assertEquals("platform", meta.getString("platform"));
        assertEquals("platform-version", meta.getString("platformVersion"));
        assertEquals("sdk-version", meta.getString("sdkVersion"));
        assertEquals("merchant-app-id", meta.getString("merchantAppId"));
        assertEquals("merchant-app-name", meta.getString("merchantAppName"));
        assertEquals("false", meta.getString("deviceRooted"));
        assertEquals("device-manufacturer", meta.getString("deviceManufacturer"));
        assertEquals("device-model", meta.getString("deviceModel"));
        assertEquals("persistent-uuid",
            meta.getString("deviceAppGeneratedPersistentUuid"));
        assertEquals("false", meta.getString("isSimulator"));
        assertEquals("user-orientation", meta.getString("userInterfaceOrientation"));
        assertEquals("sample-integration", meta.getString("integrationType"));
        assertEquals("sample-session-id", meta.getString("sessionId"));
        assertTrue(meta.getBoolean("paypalInstalled"));
        assertTrue(meta.getBoolean("venmoInstalled"));
    }

    @Test
    public void uploadAnalytics_whenEventsExist_sendsAllEvents() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);
        when(httpClient.getAuthorization()).thenReturn(authorization);

        List<AnalyticsEvent> events = new ArrayList<>();
        events.add(new AnalyticsEvent("event0", 123));
        events.add(new AnalyticsEvent("event1", 456));

        when(analyticsEventDao.getAllEvents()).thenReturn(events);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        sut.uploadAnalytics(context, inputData);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(anyString(), captor.capture(), any(Configuration.class));

        JSONObject analyticsJson = new JSONObject(captor.getValue());
        JSONArray array = analyticsJson.getJSONArray("analytics");
        assertEquals(2, array.length());

        JSONObject eventOne = array.getJSONObject(0);
        assertEquals("event0", eventOne.getString("kind"));
        assertEquals(123, Long.parseLong(eventOne.getString("timestamp")));

        JSONObject eventTwo = array.getJSONObject(1);
        assertEquals("event1", eventTwo.getString("kind"));
        assertEquals(456, Long.parseLong(eventTwo.getString("timestamp")));
    }

    @Test
    public void uploadAnalytics_deletesDatabaseEventsOnSuccessResponse() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);
        when(httpClient.getAuthorization()).thenReturn(authorization);

        List<AnalyticsEvent> events = new ArrayList<>();
        events.add(new AnalyticsEvent("event0", 123));
        events.add(new AnalyticsEvent("event1", 456));

        when(analyticsEventDao.getAllEvents()).thenReturn(events);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        sut.uploadAnalytics(context, inputData);

        verify(analyticsEventDao).deleteEvents(events);
    }

    @Test
    public void uploadAnalytics_returnsError() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);
        when(httpClient.getAuthorization()).thenReturn(authorization);

        List<AnalyticsEvent> events = new ArrayList<>();
        events.add(new AnalyticsEvent("event0", 123));
        events.add(new AnalyticsEvent("event1", 456));

        when(analyticsEventDao.getAllEvents()).thenReturn(events);

        Exception httpError = new Exception("error");
        when(httpClient.post(anyString(), anyString(), any(Configuration.class))).thenThrow(httpError);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector, analyticsDatabase);
        ListenableWorker.Result result = sut.uploadAnalytics(context, inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
    }

    private static DeviceMetadata createSampleDeviceMetadata() {
        return new DeviceMetadata.Builder()
                .integration("sample-integration")
                .sessionId("sample-session-id")
                .platform("platform")
                .sdkVersion("sdk-version")
                .deviceManufacturer("device-manufacturer")
                .deviceModel("device-model")
                .platformVersion("platform-version")
                .merchantAppName("merchant-app-name")
                .devicePersistentUUID("persistent-uuid")
                .merchantAppId("merchant-app-id")
                .userOrientation("user-orientation")
                .isPayPalInstalled(true)
                .isVenmoInstalled(true)
                .isSimulator(false)
                .isDeviceRooted(false)
                .build();
    }
}
