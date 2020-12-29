package com.braintreepayments.api.internal;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;

import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.DeviceInspector;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.Fixtures;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.awaitTasksFinished;
import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.clearAllEvents;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsSenderUnitTest {

    private Authorization mAuthorization;
    private BraintreeHttpClient mHttpClient;
    private long mCurrentTime;
    private long mOneSecondLater;
    private Configuration mConfiguration;

    @Before
    public void setup() throws InvalidArgumentException {
        mAuthorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        mHttpClient = mock(BraintreeHttpClient.class);
        mCurrentTime = System.currentTimeMillis();
        mOneSecondLater = mCurrentTime + 999;
        mConfiguration = mock(Configuration.class);
    }

    @After
    public void tearDown() {
        clearAllEvents(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void flushEvents_doesNothingIfThereAreNoEvents() {
        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", true);

        verifyZeroInteractions(mHttpClient);
    }

    @Test
    public void newRequest_sendsCorrectMetaData() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        DeviceInspector deviceInspector = mock(DeviceInspector.class);
        when(deviceInspector.isPayPalInstalled(context)).thenReturn(true);
        when(deviceInspector.isVenmoInstalled(context)).thenReturn(true);

        AnalyticsEvent event = new AnalyticsEvent(context, "sessionId", "custom", "event.started", deviceInspector);
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(context);
        database.addEvent(event);

        awaitTasksFinished(database);

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient).post(anyString(), captor.capture(), same(mConfiguration));

        JSONObject object = new JSONObject(captor.getValue());
        JSONObject meta = object.getJSONObject("_meta");

        assertEquals("Android", meta.getString("platform"));
        assertEquals(Integer.toString(VERSION.SDK_INT), meta.getString("platformVersion"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("sdkVersion"));
        // WORKAROUND: Google is appending '.test' to the package name in unit tests.
        // This works fine on an emulator.
        assertEquals("com.braintreepayments.api.test", meta.getString("merchantAppId"));
        assertEquals("Test Application", meta.getString("merchantAppName"));
        assertEquals(Build.MANUFACTURER, meta.getString("deviceManufacturer"));
        assertEquals(Build.MODEL, meta.getString("deviceModel"));
        assertEquals(UUIDHelper.getPersistentUUID(ApplicationProvider.getApplicationContext()),
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
    public void sendsAllEvents() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(ApplicationProvider.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient).post(anyString(), captor.capture(), same(mConfiguration));

        JSONObject analyticsJson = new JSONObject(captor.getValue());
        JSONArray array = analyticsJson.getJSONArray("analytics");
        assertEquals(2, array.length());
        JSONObject eventOne = array.getJSONObject(0);
        assertEquals("android.started", eventOne.getString("kind"));
        assertTrue(Long.parseLong(eventOne.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(eventOne.getString("timestamp")) <= mOneSecondLater);

        JSONObject eventTwo = array.getJSONObject(1);
        assertEquals("android.finished", eventTwo.getString("kind"));
        assertTrue(Long.parseLong(eventTwo.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(eventTwo.getString("timestamp")) <= mOneSecondLater);
    }

    @Test
    public void disambiguatesBasedOnDiscreteParams() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionIdTwo", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(ApplicationProvider.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient, times(2)).post(anyString(), captor.capture(), same(mConfiguration));

        List<String> values = captor.getAllValues();
        assertEquals(2, values.size());

        JSONObject requestJson = new JSONObject(values.get(0));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        JSONObject analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        JSONObject meta = requestJson.getJSONObject("_meta");
        assertEquals("android.started", analyticsEvent.getString("kind"));
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) <= mOneSecondLater);
        assertEquals("sessionId", meta.getString("sessionId"));

        requestJson = new JSONObject(values.get(1));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        meta = requestJson.getJSONObject("_meta");
        assertEquals("android.finished", analyticsEvent.getString("kind"));
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) <= mOneSecondLater);
        assertEquals("sessionIdTwo", meta.getString("sessionId"));
    }

    @Test
    public void deletesDatabaseEventsOnSuccessResponse() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(ApplicationProvider.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(mHttpClient.post(anyString(), anyString(), same(mConfiguration))).thenReturn("");

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", true);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(0, pendingEvents.size());
    }

    @Test
    public void deletesDatabaseEventsOnAsynchronousSuccessResponse() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(ApplicationProvider.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((HttpResponseCallback) invocation.getArguments()[3]).success("");
                return null;
            }
        }).when(mHttpClient).post(anyString(), anyString(), same(mConfiguration), any(HttpResponseCallback.class));

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", false);

        awaitTasksFinished(database);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(0, pendingEvents.size());
    }

    @Test
    public void doesNothingOnErrorResponse() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(ApplicationProvider.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(mHttpClient.post(anyString(), anyString(), same(mConfiguration))).thenThrow(ServerException.class);

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", true);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(1, pendingEvents.size());
    }

    @Test
    public void doesNothingOnAsynchronousErrorResponse() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        AnalyticsEvent one = new AnalyticsEvent(context, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(context, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(ApplicationProvider.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                ((HttpResponseCallback) invocation.getArguments()[3]).failure(new ServerException(""));
                return null;
            }
        }).when(mHttpClient).post(anyString(), anyString(), same(mConfiguration), any(HttpResponseCallback.class));

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), mAuthorization, mConfiguration, mHttpClient, "", false);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(1, pendingEvents.size());
    }
}
