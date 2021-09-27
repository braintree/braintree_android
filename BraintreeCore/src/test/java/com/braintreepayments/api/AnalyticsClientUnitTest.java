package com.braintreepayments.api;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.impl.model.WorkSpec;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.braintreepayments.api.AnalyticsDatabaseTestUtils.awaitTasksFinished;
import static com.braintreepayments.api.AnalyticsDatabaseTestUtils.clearAllEvents;
import static com.braintreepayments.api.SharedPreferencesHelper.getSharedPreferences;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsClientUnitTest {

    private Context context;
    private Authorization authorization;

    private BraintreeHttpClient httpClient;
    private DeviceInspector deviceInspector;
    private ClassHelper classHelper;

    private long mCurrentTime;
    private BraintreeSharedPreferences braintreeSharedPreferences;

    @Before
    public void setup() throws InvalidArgumentException, GeneralSecurityException, IOException {
        authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        mCurrentTime = System.currentTimeMillis();

        context = ApplicationProvider.getApplicationContext();

        httpClient = mock(BraintreeHttpClient.class);
        deviceInspector = mock(DeviceInspector.class);
        classHelper = mock(ClassHelper.class);

        braintreeSharedPreferences = mock(BraintreeSharedPreferences.class);
        when(braintreeSharedPreferences.getSharedPreferences(context)).thenReturn(getSharedPreferences(context));

        WorkManagerTestInitHelper.initializeTestWorkManager(context);
    }

    @After
    public void tearDown() {
        clearAllEvents(context);
    }

    @Test
    public void createAnalyticsWorkerRequest_returnsAnalyticsUploadWorkerWithDelay() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        OneTimeWorkRequest result = AnalyticsClient.createAnalyticsWorkerRequest(configuration, authorization);

        WorkSpec workSpec = result.getWorkSpec();
        assertEquals(30000, workSpec.initialDelay);
        assertEquals(AnalyticsUploadWorker.class.getName(), workSpec.workerClassName);

        assertEquals(configuration.toJson(), workSpec.input.getString("configuration"));
        assertEquals(authorization.toString(), workSpec.input.getString("authorization"));
    }

    @Test
    public void sendEvent_addsEventToAnalyticsDatabase() throws JSONException, InterruptedException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        AnalyticsEvent event = new AnalyticsEvent(
                context, "sessionId", "custom", "event.started", deviceInspector, classHelper);

        when(httpClient.getAuthorization()).thenReturn(authorization);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        sut.sendEvent(context, configuration, event);

        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context);
        awaitTasksFinished(database);

        AnalyticsEvent capturedEvent = database.getPendingRequests().get(0).get(0);
        assertEquals(event.timestamp, capturedEvent.timestamp);
    }

    @Test
    public void sendEvent_setsLastKnownAnalyticsUrl() throws JSONException {
        when(httpClient.getAuthorization()).thenReturn(authorization);

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        AnalyticsEvent event = new AnalyticsEvent(
                context, "sessionId", "custom", "event.started", deviceInspector, classHelper);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        sut.sendEvent(context, configuration, event);

        assertEquals("analytics_url", sut.getLastKnownAnalyticsUrl());
    }

    @Test
    public void sendEventAndReturnId_enqueuesAnalyticsWorker() throws ExecutionException, InterruptedException, JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        AnalyticsEvent event = new AnalyticsEvent(
                context, "sessionId", "custom", "event.started", deviceInspector, classHelper);

        when(httpClient.getAuthorization()).thenReturn(authorization);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        UUID workSpecId = sut.sendEventAndReturnId(context, configuration, event);

        WorkInfo analyticsWorkerInfo = WorkManager.getInstance(context).getWorkInfoById(workSpecId).get();
        assertEquals(WorkInfo.State.ENQUEUED, analyticsWorkerInfo.getState());
    }

    @Test
    public void uploadAnalytics_whenNoEventsExist_doesNothing() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        sut.uploadAnalytics(context, configuration, braintreeSharedPreferences);

        verifyZeroInteractions(httpClient);
    }

    @Test
    public void uploadAnalytics_whenEventsExist_sendsCorrectMetaData() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        when(deviceInspector.isPayPalInstalled(context)).thenReturn(true);
        when(deviceInspector.isVenmoInstalled(context)).thenReturn(true);

        AnalyticsEvent event = new AnalyticsEvent(context, "sessionId", "custom", "event.started", deviceInspector, classHelper);
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context);
        database.addEvent(event);

        awaitTasksFinished(database);

        when(deviceInspector.getAppName(context)).thenReturn("Test Application");
        when(deviceInspector.isDeviceEmulator()).thenReturn(false);
        when(deviceInspector.isDeviceRooted()).thenReturn(false);

        when(httpClient.getAuthorization()).thenReturn(authorization);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        sut.uploadAnalytics(context, configuration, braintreeSharedPreferences);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(anyString(), captor.capture(), same(configuration));
        JSONObject object = new JSONObject(captor.getValue());
        JSONObject meta = object.getJSONObject("_meta");

        assertEquals("Android", meta.getString("platform"));
        assertEquals(Integer.toString(VERSION.SDK_INT), meta.getString("platformVersion"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("sdkVersion"));
        // WORKAROUND: Google is appending '.test' to the package name in unit tests.
        // This works fine on an emulator.
        assertEquals("com.braintreepayments.api.test", meta.getString("merchantAppId"));
        assertEquals("Test Application", meta.getString("merchantAppName"));
        assertEquals("false", meta.getString("deviceRooted"));
        assertEquals(Build.MANUFACTURER, meta.getString("deviceManufacturer"));
        assertEquals(Build.MODEL, meta.getString("deviceModel"));
        assertEquals(UUIDHelper.getPersistentUUID(context, braintreeSharedPreferences),
                meta.getString("deviceAppGeneratedPersistentUuid"));
        assertEquals("false", meta.getString("isSimulator"));
        assertEquals("Portrait", meta.getString("userInterfaceOrientation"));
        assertEquals("custom", meta.getString("integrationType"));
        assertEquals("sessionId", meta.getString("sessionId"));
        assertFalse(meta.getString("sessionId").contains("-"));
        assertTrue(meta.getBoolean("paypalInstalled"));
        assertTrue(meta.getBoolean("venmoInstalled"));
    }

    @Test
    public void uploadAnalytics_whenEventsExist_sendsAllEvents() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(httpClient.getAuthorization()).thenReturn(authorization);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        sut.uploadAnalytics(context, configuration, braintreeSharedPreferences);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(anyString(), captor.capture(), same(configuration));

        JSONObject analyticsJson = new JSONObject(captor.getValue());
        JSONArray array = analyticsJson.getJSONArray("analytics");
        assertEquals(2, array.length());
        JSONObject eventOne = array.getJSONObject(0);
        assertEquals("android.started", eventOne.getString("kind"));
        assertTrue(Long.parseLong(eventOne.getString("timestamp")) >= mCurrentTime);

        JSONObject eventTwo = array.getJSONObject(1);
        assertEquals("android.finished", eventTwo.getString("kind"));
        assertTrue(Long.parseLong(eventTwo.getString("timestamp")) >= mCurrentTime);
    }

    @Test
    public void uploadAnalytics_whenEventsExist_batchesUploadsBySessionId() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionIdTwo", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(httpClient.getAuthorization()).thenReturn(authorization);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        sut.uploadAnalytics(context, configuration, braintreeSharedPreferences);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(httpClient, times(2)).post(anyString(), captor.capture(), same(configuration));

        List<String> values = captor.getAllValues();
        assertEquals(2, values.size());

        JSONObject requestJson = new JSONObject(values.get(0));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        JSONObject analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        JSONObject meta = requestJson.getJSONObject("_meta");
        assertEquals("android.started", analyticsEvent.getString("kind"));
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) >= mCurrentTime);
        assertEquals("sessionId", meta.getString("sessionId"));

        requestJson = new JSONObject(values.get(1));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        meta = requestJson.getJSONObject("_meta");
        assertEquals("android.finished", analyticsEvent.getString("kind"));
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) >= mCurrentTime);
        assertEquals("sessionIdTwo", meta.getString("sessionId"));
    }

    @Test
    public void uploadAnalytics_deletesDatabaseEventsOnSuccessResponse() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(httpClient.getAuthorization()).thenReturn(authorization);
        when(httpClient.post(anyString(), anyString(), same(configuration))).thenReturn("");

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        sut.uploadAnalytics(context, configuration, braintreeSharedPreferences);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(0, pendingEvents.size());
    }

    @Test
    public void uploadAnalytics_propagatesExceptionsOnError() throws Exception {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(httpClient.getAuthorization()).thenReturn(authorization);

        Exception httpError = new Exception("error");
        when(httpClient.post(anyString(), anyString(), same(configuration))).thenThrow(httpError);

        AnalyticsClient sut = new AnalyticsClient(httpClient, deviceInspector);
        try {
            sut.uploadAnalytics(context, configuration, braintreeSharedPreferences);
            fail("uploadAnalytics should throw");
        } catch (Exception e) {
            assertSame(httpError, e);
            assertEquals(1, database.getPendingRequests().size());
        }
    }
}
