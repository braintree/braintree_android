package com.braintreepayments.api.internal;

import android.os.Build;
import android.os.Build.VERSION;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

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
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.List;

import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.awaitTasksFinished;
import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.clearAllEvents;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
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
    private long mTwoSecondsLater;

    @Before
    public void setup() throws InvalidArgumentException {
        mAuthorization = Authorization.fromString(TOKENIZATION_KEY);
        mHttpClient = mock(BraintreeHttpClient.class);
        mCurrentTime = System.currentTimeMillis();
        mTwoSecondsLater = mCurrentTime + 2000;
    }

    @After
    public void tearDown() throws IOException {
        clearAllEvents(RuntimeEnvironment.application);
    }

    @Test
    public void flushEvents_doesNothingIfThereAreNoEvents() {
        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", true);

        verifyZeroInteractions(mHttpClient);
    }

    @Test
    public void newRequest_sendsCorrectMetaData() throws Exception {
        AnalyticsEvent event = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom",
                "event.started");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        database.addEvent(event);

        awaitTasksFinished(database);

        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient).post(anyString(), captor.capture());

        JSONObject object = new JSONObject(captor.getValue());
        JSONObject meta = object.getJSONObject("_meta");

        assertEquals("Android", meta.getString("platform"));
        assertEquals(Integer.toString(VERSION.SDK_INT), meta.getString("platformVersion"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("sdkVersion"));
        assertEquals("com.braintreepayments.api", meta.getString("merchantAppId"));
        assertEquals("com.braintreepayments.api", meta.getString("merchantAppName"));
        assertEquals(Build.MANUFACTURER, meta.getString("deviceManufacturer"));
        assertEquals(Build.MODEL, meta.getString("deviceModel"));
        assertEquals(UUIDHelper.getPersistentUUID(RuntimeEnvironment.application),
                meta.getString("deviceAppGeneratedPersistentUuid"));
        assertEquals("false", meta.getString("isSimulator"));
        assertEquals("Portrait", meta.getString("userInterfaceOrientation"));
        assertEquals("custom", meta.getString("integrationType"));
        assertEquals("sessionId", meta.getString("sessionId"));
        assertFalse(meta.getString("sessionId").contains("-"));
        assertEquals(PayPalOneTouchCore.isWalletAppInstalled(RuntimeEnvironment.application),
                meta.getBoolean("paypalInstalled"));
        assertEquals(Venmo.isVenmoInstalled(RuntimeEnvironment.application),
                meta.getBoolean("venmoInstalled"));
    }

    @Test
    public void sendsAllEvents() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient).post(anyString(), captor.capture());

        JSONObject analyticsJson = new JSONObject(captor.getValue());
        JSONArray array = analyticsJson.getJSONArray("analytics");
        assertEquals(2, array.length());
        JSONObject eventOne = array.getJSONObject(0);
        assertEquals("android.started", eventOne.getString("kind"));
        assertTrue(Long.parseLong(eventOne.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(eventOne.getString("timestamp")) <= mTwoSecondsLater);

        JSONObject eventTwo = array.getJSONObject(1);
        assertEquals("android.finished", eventTwo.getString("kind"));
        assertTrue(Long.parseLong(eventTwo.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(eventTwo.getString("timestamp")) <= mTwoSecondsLater);
    }

    @Test
    public void disambiguatesBasedOnDiscreteParams() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(RuntimeEnvironment.application, "sessionIdTwo", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient, times(2)).post(anyString(), captor.capture());

        List<String> values = captor.getAllValues();
        assertEquals(2, values.size());

        JSONObject requestJson = new JSONObject(values.get(0));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        JSONObject analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        JSONObject meta = requestJson.getJSONObject("_meta");
        assertEquals("android.started", analyticsEvent.getString("kind"));
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) <= mTwoSecondsLater);        assertEquals("sessionId", meta.getString("sessionId"));

        requestJson = new JSONObject(values.get(1));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        meta = requestJson.getJSONObject("_meta");
        assertEquals("android.finished", analyticsEvent.getString("kind"));
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) >= mCurrentTime);
        assertTrue(Long.parseLong(analyticsEvent.getString("timestamp")) <= mTwoSecondsLater);        assertEquals("sessionIdTwo", meta.getString("sessionId"));
    }

    @Test
    public void deletesDatabaseEventsOnSuccessResponse() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(mHttpClient.post(anyString(), anyString())).thenReturn("");

        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", true);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(0, pendingEvents.size());
    }

    @Test
    public void deletesDatabaseEventsOnAsynchronousSuccessResponse() throws InterruptedException {
        AnalyticsEvent one = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((HttpResponseCallback) invocation.getArguments()[2]).success("");
                return null;
            }
        }).when(mHttpClient).post(anyString(), anyString(), any(HttpResponseCallback.class));

        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", false);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(0, pendingEvents.size());
    }

    @Test
    public void doesNothingOnErrorResponse() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        when(mHttpClient.post(anyString(), anyString())).thenThrow(ServerException.class);

        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", true);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(1, pendingEvents.size());
    }

    @Test
    public void doesNothingOnAsynchronousErrorResponse() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(RuntimeEnvironment.application, "sessionId", "custom", "finished");
        AnalyticsDatabase database = AnalyticsDatabase.getInstance(RuntimeEnvironment.application);
        database.addEvent(one);
        database.addEvent(two);

        awaitTasksFinished(database);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((HttpResponseCallback) invocation.getArguments()[2]).failure(new ServerException(""));
                return null;
            }
        }).when(mHttpClient).post(anyString(), anyString(), any(HttpResponseCallback.class));

        AnalyticsSender.send(RuntimeEnvironment.application, mAuthorization, mHttpClient, "", false);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(1, pendingEvents.size());
    }
}
