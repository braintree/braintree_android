package com.braintreepayments.api.internal;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.ClientKey;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AnalyticsManagerTest {

    @Before
    public void setup() throws JSONException {
        ClientToken clientToken = ClientToken.fromString(stringFromFixture("client_token.json"));
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration_with_analytics.json"));
        AnalyticsManager.setup(getTargetContext(), configuration.getAnalytics(), clientToken);
    }

    @Test(timeout = 1000)
    public void sendEvent_worksWithClientKey() throws InvalidArgumentException {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        AnalyticsManager.setup(getTargetContext(), analyticsConfiguration,
                ClientKey.fromString(CLIENT_KEY));
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);

        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.flushEvents();

        verify(AnalyticsManager.sHttpClient)
                .post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void sendEvent_doesNothingIfAnalyticsNotEnabled()
            throws InvalidArgumentException {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(false);
        AnalyticsManager.setup(getTargetContext(), analyticsConfiguration,
                ClientKey.fromString(CLIENT_KEY));
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);

        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");

        verify(AnalyticsManager.sHttpClient, never())
                .post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void sendEvent_doesNotSendEventIfBatchSizeNotReached() throws JSONException {
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);

        AnalyticsManager.sendRequest("custom", "some-interesting-event");

        verify(AnalyticsManager.sHttpClient, never())
                .post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void sendEvent_sendsEventAfterBatchSizeReached() throws JSONException {
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);

        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(AnalyticsManager.sHttpClient)
                .post(anyString(), captor.capture(), isNull(HttpResponseCallback.class));
        JSONObject json = new JSONObject(captor.getValue());
        assertEquals(5, json.getJSONArray("analytics").length());
        assertEquals("custom.android.some-interesting-event", json.getJSONArray("analytics").getJSONObject(0).get("kind"));
    }

    @Test(timeout = 1000)
    public void sendEvent_sendsMultipleRequestsWhenBatchSizeReached() {
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);

        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.sendRequest("custom", "some-interesting-event");

        verify(AnalyticsManager.sHttpClient, times(2))
                .post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void flushEvents_doesNothingIfThereAreNoEvents() {
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);
        AnalyticsManager.flushEvents();

        verify(AnalyticsManager.sHttpClient, never())
                .post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void flushEvents_sendsAllEventsRegardlessOfNumber() {
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);

        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.flushEvents();

        verify(AnalyticsManager.sHttpClient)
                .post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void newRequest_sendsCorrectMetaData() throws JSONException {
        AnalyticsManager.sHttpClient = mock(BraintreeHttpClient.class);
        String uuid = getTargetContext().getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE)
                .getString("braintreeUUID", null);

        AnalyticsManager.sendRequest("custom", "some-interesting-event");
        AnalyticsManager.flushEvents();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(AnalyticsManager.sHttpClient)
                .post(anyString(), captor.capture(), isNull(HttpResponseCallback.class));
        JSONObject json = new JSONObject(captor.getValue()).getJSONObject("_meta");
        assertEquals("Android", json.getString("platform"));
        assertEquals(Integer.toString(VERSION.SDK_INT), json.getString("platformVersion"));
        assertEquals(BuildConfig.VERSION_NAME, json.getString("sdkVersion"));
        assertEquals("com.braintreepayments.api.test", json.getString("merchantAppId"));
        assertEquals("Test-api", json.getString("merchantAppName"));
        assertEquals(Build.MANUFACTURER, json.getString("deviceManufacturer"));
        assertEquals(Build.MODEL, json.getString("deviceModel"));
        assertEquals(Secure.getString(getTargetContext().getContentResolver(), Secure.ANDROID_ID), json.getString("androidId"));
        assertEquals(uuid, json.getString("deviceAppGeneratedPersistentUuid"));
        assertEquals("true", json.getString("isSimulator"));
        assertEquals("Portrait", json.getString("userInterfaceOrientation"));
        assertEquals("custom", json.getString("integrationType"));
    }
}
