package com.braintreepayments.api.internal;

import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.Venmo;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.testutils.TestTokenizationKey;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.List;

import static com.braintreepayments.api.internal.AnalyticsDatabaseTestUtils.clearAllEvents;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(RobolectricGradleTestRunner.class)
public class AnalyticsIntentServiceUnitTest {

    private static final String AUTHORIZATION = TestTokenizationKey.TOKENIZATION_KEY;

    private AnalyticsIntentService mService;
    private Intent mServiceIntent;

    @Before
    public void setup() throws IOException {
        mService = new AnalyticsIntentService();
        mService.onCreate();
        mService.mHttpClient = mock(BraintreeHttpClient.class);
        mServiceIntent = new Intent(RuntimeEnvironment.application, AnalyticsIntentService.class)
            .putExtra(AnalyticsIntentService.EXTRA_CONFIGURATION, new TestConfigurationBuilder().build())
            .putExtra(AnalyticsIntentService.EXTRA_AUTHORIZATION, AUTHORIZATION);
    }

    @After
    public void tearDown() throws IOException {
        clearAllEvents(RuntimeEnvironment.application);
    }

    @Test
    public void flushEvents_doesNothingIfThereAreNoEvents() {
        mService.onHandleIntent(mServiceIntent);
        verifyZeroInteractions(mService.mHttpClient);
    }

    @Test
    public void newRequest_sendsCorrectMetaData() throws Exception {
        AnalyticsEvent event = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "event.started");

        AnalyticsDatabase database = AnalyticsDatabase.getInstance(mService.getApplicationContext());
        database.addEvent(event);

        mService.onHandleIntent(mServiceIntent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mService.mHttpClient).post(anyString(), captor.capture());

        JSONObject object = new JSONObject(captor.getValue());
        JSONObject meta = object.getJSONObject("_meta");

        assertEquals("Android", meta.getString("platform"));
        assertEquals(Integer.toString(VERSION.SDK_INT), meta.getString("platformVersion"));
        assertEquals(BuildConfig.VERSION_NAME, meta.getString("sdkVersion"));
        assertEquals("com.braintreepayments.api", meta.getString("merchantAppId"));
        assertEquals("ApplicationNameUnknown", meta.getString("merchantAppName"));
        assertEquals(Build.MANUFACTURER, meta.getString("deviceManufacturer"));
        assertEquals(Build.MODEL, meta.getString("deviceModel"));
        assertEquals("AndroidIdUnknown", meta.getString("androidId"));
        assertEquals(UUIDHelper.getPersistentUUID(mService.getApplicationContext()),
                meta.getString("deviceAppGeneratedPersistentUuid"));
        assertEquals("false", meta.getString("isSimulator"));
        assertEquals("Unknown", meta.getString("userInterfaceOrientation"));
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
        AnalyticsEvent one = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "finished");

        AnalyticsDatabase database = AnalyticsDatabase.getInstance(mService.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        mService.onHandleIntent(mServiceIntent);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mService.mHttpClient).post(anyString(), captor.capture());

        JSONObject analyticsJson = new JSONObject(captor.getValue());
        JSONArray array = analyticsJson.getJSONArray("analytics");
        assertEquals(2, array.length());
        JSONObject eventOne = array.getJSONObject(0);
        assertEquals("android.custom.started", eventOne.getString("kind"));

        JSONObject eventTwo = array.getJSONObject(1);
        assertEquals("android.custom.finished", eventTwo.getString("kind"));
    }

    @Test
    public void disambiguatesBasedOnDiscreteParams() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionIdTwo", "custom", "finished");

        AnalyticsDatabase database = AnalyticsDatabase.getInstance(mService.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        mService.onHandleIntent(mServiceIntent);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mService.mHttpClient, times(2)).post(anyString(), captor.capture());

        List<String> values = captor.getAllValues();
        assertEquals(2, values.size());

        JSONObject requestJson = new JSONObject(values.get(0));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        JSONObject analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        JSONObject meta = requestJson.getJSONObject("_meta");
        assertEquals("android.custom.started", analyticsEvent.getString("kind"));
        assertEquals("sessionId", meta.getString("sessionId"));

        requestJson = new JSONObject(values.get(1));
        assertEquals(1, requestJson.getJSONArray("analytics").length());
        analyticsEvent = requestJson.getJSONArray("analytics").getJSONObject(0);
        meta = requestJson.getJSONObject("_meta");
        assertEquals("android.custom.finished", analyticsEvent.getString("kind"));
        assertEquals("sessionIdTwo", meta.getString("sessionId"));
    }

    @Test
    public void deletesDatabaseEventsOnSuccessResponse() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "finished");

        final AnalyticsDatabase database = AnalyticsDatabase.getInstance(mService.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        when(mService.mHttpClient.post(anyString(), anyString())).thenReturn("");
        mService.onHandleIntent(mServiceIntent);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(0, pendingEvents.size());
    }

    @Test
    public void doesNothingOnErrorResponse() throws Exception {
        AnalyticsEvent one = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "started");
        AnalyticsEvent two = new AnalyticsEvent(mService.getApplicationContext(),
                "sessionId", "custom", "finished");

        final AnalyticsDatabase database = AnalyticsDatabase.getInstance(mService.getApplicationContext());
        database.addEvent(one);
        database.addEvent(two);

        when(mService.mHttpClient.post(anyString(), anyString())).thenThrow(ServerException.class);
        mService.onHandleIntent(mServiceIntent);

        List<List<AnalyticsEvent>> pendingEvents = database.getPendingRequests();
        assertEquals(1, pendingEvents.size());
    }
}
