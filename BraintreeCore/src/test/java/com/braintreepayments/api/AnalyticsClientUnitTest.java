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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.impl.model.WorkSpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

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

    private WorkManager workManager;
    private AnalyticsDatabase analyticsDatabase;
    private AnalyticsEventDao analyticsEventDao;

    @Before
    public void beforeEach() throws InvalidArgumentException, GeneralSecurityException, IOException {
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

        workManager = mock(WorkManager.class);
    }

    @Test
    public void sendEvent_enqueuesAnalyticsWriteToDbWorker() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        sut.sendEvent(configuration, eventName, sessionId, integration, 123, authorization);

        ArgumentCaptor<OneTimeWorkRequest> captor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(workManager)
                .enqueueUniqueWork(eq("writeAnalyticsToDb"), eq(ExistingWorkPolicy.APPEND_OR_REPLACE), captor.capture());

        OneTimeWorkRequest workRequest = captor.getValue();
        WorkSpec workSpec = workRequest.getWorkSpec();
        assertEquals(AnalyticsWriteToDbWorker.class.getName(), workSpec.workerClassName);

        assertEquals(authorization.toString(), workSpec.input.getString("authorization"));
        assertEquals("android.sample-event-name", workSpec.input.getString("eventName"));
        assertEquals(123, workSpec.input.getLong("timestamp", 0));
    }

    @Test
    public void sendEvent_enqueuesAnalyticsUploadWorker() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        sut.sendEvent(configuration, eventName, sessionId, integration, 123, authorization);

        ArgumentCaptor<OneTimeWorkRequest> captor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
        verify(workManager)
                .enqueueUniqueWork(eq("uploadAnalytics"), eq(ExistingWorkPolicy.KEEP), captor.capture());

        OneTimeWorkRequest workRequest = captor.getValue();
        WorkSpec workSpec = workRequest.getWorkSpec();
        assertEquals(30000, workSpec.initialDelay);
        assertEquals(AnalyticsUploadWorker.class.getName(), workSpec.workerClassName);

        assertEquals(configuration.toJson(), workSpec.input.getString("configuration"));
        assertEquals(authorization.toString(), workSpec.input.getString("authorization"));
        assertEquals("sample-session-id", workSpec.input.getString("sessionId"));
        assertEquals("sample-integration", workSpec.input.getString("integration"));
    }

    @Test
    public void writeAnalytics_whenEventNameAndTimestampArePresent_returnsSuccess() {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
                .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        ListenableWorker.Result result = sut.writeAnalytics(inputData);
        assertTrue(result instanceof ListenableWorker.Result.Success);
    }

    @Test
    public void writeAnalytics_whenEventNameIsMissing_returnsFailure() {
        Data inputData = new Data.Builder()
                .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        ListenableWorker.Result result = sut.writeAnalytics(inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
    }

    @Test
    public void writeAnalytics_whenTimestampIsMissing_returnsFailure() {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        ListenableWorker.Result result = sut.writeAnalytics(inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
    }

    @Test
    public void writeAnalytics_addsEventToAnalyticsDatabaseAndReturnsSuccess() {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_EVENT_NAME, eventName)
                .putLong(WORK_INPUT_KEY_TIMESTAMP, timestamp)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        sut.writeAnalytics(inputData);

        ArgumentCaptor<AnalyticsEvent> captor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventDao).insertEvent(captor.capture());

        AnalyticsEvent event = captor.getValue();
        assertEquals("sample-event-name", event.getName());
        assertEquals(123, event.getTimestamp());
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

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        sut.uploadAnalytics(context, inputData);

        verifyZeroInteractions(httpClient);
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

        List<AnalyticsEvent> events = new ArrayList<>();
        events.add(new AnalyticsEvent("event0", 123));
        events.add(new AnalyticsEvent("event1", 456));

        when(analyticsEventDao.getAllEvents()).thenReturn(events);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        sut.uploadAnalytics(context, inputData);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(anyString(), captor.capture(), any(Configuration.class), any(Authorization.class));

        JSONObject analyticsJson = new JSONObject(captor.getValue());

        JSONObject meta = analyticsJson.getJSONObject("_meta");
        JSONAssert.assertEquals(metadata.toJSON(), meta, true);

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
    public void uploadAnalytics_whenConfigurationIsNull_doesNothing() {
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);

        ListenableWorker.Result result = sut.uploadAnalytics(context, inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void uploadAnalytics_whenAuthorizationIsNull_doesNothing() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);

        ListenableWorker.Result result = sut.uploadAnalytics(context, inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void uploadAnalytics_whenSessionIdIsNull_doesNothing() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);

        ListenableWorker.Result result = sut.uploadAnalytics(context, inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void uploadAnalytics_whenIntegrationIsNull_doesNothing() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .build();

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);

        ListenableWorker.Result result = sut.uploadAnalytics(context, inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
        verifyZeroInteractions(httpClient);
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

        List<AnalyticsEvent> events = new ArrayList<>();
        events.add(new AnalyticsEvent("event0", 123));
        events.add(new AnalyticsEvent("event1", 456));

        when(analyticsEventDao.getAllEvents()).thenReturn(events);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        sut.uploadAnalytics(context, inputData);

        verify(analyticsEventDao).deleteEvents(events);
    }

    @Test
    public void uploadAnalytics_whenAnalyticsSendFails_returnsError() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        Data inputData = new Data.Builder()
                .putString(WORK_INPUT_KEY_AUTHORIZATION, authorization.toString())
                .putString(WORK_INPUT_KEY_CONFIGURATION, configuration.toJson())
                .putString(WORK_INPUT_KEY_SESSION_ID, sessionId)
                .putString(WORK_INPUT_KEY_INTEGRATION, integration)
                .build();

        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);

        List<AnalyticsEvent> events = new ArrayList<>();
        events.add(new AnalyticsEvent("event0", 123));
        events.add(new AnalyticsEvent("event1", 456));

        when(analyticsEventDao.getAllEvents()).thenReturn(events);

        Exception httpError = new Exception("error");
        when(httpClient.post(anyString(), anyString(), any(Configuration.class), any(Authorization.class))).thenThrow(httpError);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        ListenableWorker.Result result = sut.uploadAnalytics(context, inputData);
        assertTrue(result instanceof ListenableWorker.Result.Failure);
    }

    @Test
    public void reportCrash_whenLastKnownAnalyticsUrlExists_sendsCrashAnalyticsEvent() throws Exception {
        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        sut.sendEvent(configuration, eventName, sessionId, integration, authorization);
        sut.reportCrash(context, sessionId, integration, 123, authorization);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(eq("analytics_url"), captor.capture(), (Configuration) isNull(), same(authorization), any(HttpNoResponse.class));

        JSONObject analyticsJson = new JSONObject(captor.getValue());

        JSONObject meta = analyticsJson.getJSONObject("_meta");
        JSONAssert.assertEquals(metadata.toJSON(), meta, true);

        JSONArray array = analyticsJson.getJSONArray("analytics");
        assertEquals(1, array.length());

        JSONObject eventOne = array.getJSONObject(0);
        assertEquals("android.crash", eventOne.getString("kind"));
        assertEquals(123, Long.parseLong(eventOne.getString("timestamp")));
    }

    @Test
    public void reportCrash_whenLastKnownAnalyticsUrlMissing_doesNothing() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        sut.sendEvent(configuration, eventName, sessionId, integration, authorization);

        sut.reportCrash(context, sessionId, integration, 123, null);
        verifyZeroInteractions(httpClient);
    }

    @Test
    public void reportCrash_whenAuthorizationIsNull_doesNothing() throws JSONException {
        DeviceMetadata metadata = createSampleDeviceMetadata();
        when(deviceInspector.getDeviceMetadata(context, sessionId, integration)).thenReturn(metadata);

        AnalyticsClient sut = new AnalyticsClient(httpClient, analyticsDatabase, workManager, deviceInspector);
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        sut.sendEvent(configuration, eventName, sessionId, integration, authorization);
        sut.reportCrash(context, sessionId, integration, 123, null);
        verifyZeroInteractions(httpClient);
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
                .build();
    }
}
