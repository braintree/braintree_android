package com.braintreepayments.api;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.BraintreeHttpClient;
import com.braintreepayments.api.models.AnalyticsConfiguration;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
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

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private BraintreeFragment mFragment;
    private BraintreeHttpClient mHttpClient;

    @Test(timeout = 1000)
    public void sendEvent_worksWithClientKey() throws InvalidArgumentException {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        mFragment = getMockFragment(mActivityTestRule.getActivity(), CLIENT_KEY,
                configuration);
        mHttpClient = mock(BraintreeHttpClient.class);
        when(mFragment.getHttpClient()).thenReturn(mHttpClient);

        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.flushEvents(mFragment);

        verify(mHttpClient).post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void sendEvent_doesNothingIfAnalyticsNotEnabled() throws InvalidArgumentException {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(false);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        mFragment = getMockFragment(mActivityTestRule.getActivity(), CLIENT_KEY, configuration);
        mHttpClient = mock(BraintreeHttpClient.class);
        when(mFragment.getHttpClient()).thenReturn(mHttpClient);

        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.flushEvents(mFragment);

        verify(mHttpClient, never()).post(anyString(), anyString(),
                isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void sendEvent_doesNotSendEventIfBatchSizeNotReached() throws JSONException {
        setup();

        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");

        verify(mHttpClient, never()).post(anyString(), anyString(),
                isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void sendEvent_sendsEventAfterBatchSizeReached() throws JSONException {
        setup();

        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient).post(anyString(), captor.capture(),
                isNull(HttpResponseCallback.class));
        JSONObject json = new JSONObject(captor.getValue());
        assertEquals(5, json.getJSONArray("analytics").length());
        assertEquals("android.custom.some-interesting-event",
                json.getJSONArray("analytics").getJSONObject(0).get("kind"));
    }

    @Test(timeout = 1000)
    public void sendEvent_sendsMultipleRequestsWhenBatchSizeReached() throws JSONException {
        setup();

        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");

        verify(mHttpClient, times(2))
                .post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void flushEvents_doesNothingIfThereAreNoEvents() throws JSONException {
        setup();

        AnalyticsManager.flushEvents(mFragment);

        verify(mHttpClient, never()).post(anyString(), anyString(),
                isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void flushEvents_sendsAllEventsRegardlessOfNumber() throws JSONException {
        setup();

        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.flushEvents(mFragment);

        verify(mHttpClient).post(anyString(), anyString(), isNull(HttpResponseCallback.class));
    }

    @Test(timeout = 1000)
    public void newRequest_sendsCorrectMetaData() throws JSONException {
        setup();
        String uuid = "testuuid";
        getTargetContext().getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE).edit()
                .putString("braintreeUUID", uuid)
                .commit();

        AnalyticsManager.sendRequest(mFragment, "custom", "some-interesting-event");
        AnalyticsManager.flushEvents(mFragment);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mHttpClient).post(anyString(), captor.capture(), isNull(HttpResponseCallback.class));
        JSONObject json = new JSONObject(captor.getValue()).getJSONObject("_meta");
        assertEquals("Android", json.getString("platform"));
        assertEquals(Integer.toString(VERSION.SDK_INT), json.getString("platformVersion"));
        assertEquals(BuildConfig.VERSION_NAME, json.getString("sdkVersion"));
        assertEquals("com.braintreepayments.api.test", json.getString("merchantAppId"));
        assertEquals("com.braintreepayments.api.test", json.getString("merchantAppName"));
        assertEquals(Build.MANUFACTURER, json.getString("deviceManufacturer"));
        assertEquals(Build.MODEL, json.getString("deviceModel"));
        assertEquals(Secure.getString(getTargetContext().getContentResolver(), Secure.ANDROID_ID), json.getString("androidId"));
        assertEquals(uuid, json.getString("deviceAppGeneratedPersistentUuid"));
        assertEquals("true", json.getString("isSimulator"));
        assertEquals("Portrait", json.getString("userInterfaceOrientation"));
        assertEquals("custom", json.getString("integrationType"));
    }

    /* helper */
    private void setup() throws JSONException {
        AnalyticsConfiguration analyticsConfiguration = mock(AnalyticsConfiguration.class);
        when(analyticsConfiguration.isEnabled()).thenReturn(true);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getAnalytics()).thenReturn(analyticsConfiguration);
        mFragment = getMockFragment(mActivityTestRule.getActivity(),
                stringFromFixture("client_token.json"), configuration);
        mHttpClient = mock(BraintreeHttpClient.class);
        when(mFragment.getHttpClient()).thenReturn(mHttpClient);
    }
}
