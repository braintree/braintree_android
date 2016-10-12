package com.paypal.android.sdk.onetouch.core.fpti;

import android.content.pm.PackageManager.NameNotFoundException;

import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;
import com.paypal.android.sdk.onetouch.core.enums.Protocol;
import com.paypal.android.sdk.onetouch.core.network.EnvironmentManager;
import com.paypal.android.sdk.onetouch.core.network.PayPalHttpClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricGradleTestRunner;

import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;

import static com.paypal.android.sdk.onetouch.core.test.TestSetupHelper.getMockContextInspector;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricGradleTestRunner.class)
public class FptiManagerUnitTest {

    private ContextInspector mContextInspector;
    private PayPalHttpClient mHttpClient;
    private FptiManager mFptiManager;

    @Before
    public void setup() throws NameNotFoundException {
        mContextInspector = getMockContextInspector();
        mHttpClient = mock(PayPalHttpClient.class);
        mFptiManager = spy(new FptiManager(mContextInspector, mHttpClient));
    }

    @Test
    public void trackFpti_doesNothingInMock() {
        mFptiManager.trackFpti(TrackingPoint.Cancel, EnvironmentManager.MOCK,
                new HashMap<String, String>(), Protocol.v1);

        verify(mFptiManager, never()).sendRequest(anyString());
    }

    @Test
    public void trackFpti_sendsRequestForSandbox() {
        mFptiManager.trackFpti(TrackingPoint.Cancel, EnvironmentManager.SANDBOX,
                new HashMap<String, String>(), Protocol.v1);

        verify(mFptiManager).sendRequest(anyString());
    }

    @Test
    public void trackFpti_sendsRequestForLive() {
        mFptiManager.trackFpti(TrackingPoint.Cancel, EnvironmentManager.LIVE,
                new HashMap<String, String>(), Protocol.v1);

        verify(mFptiManager).sendRequest(anyString());
    }

    @Test
    public void trackFpti_sendsRequest() throws JSONException {
        mFptiManager.trackFpti(TrackingPoint.Cancel, EnvironmentManager.SANDBOX,
                new HashMap<String, String>(), Protocol.v1);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mFptiManager).sendRequest(captor.capture());

        JSONObject events = new JSONObject(captor.getValue()).getJSONObject("events");
        assertNotNull(events.getString("tracking_event"));
        assertEquals("mobile", events.getString("channel"));

        JSONObject actor = events.getJSONObject("actor");
        assertEquals("installation-guid", actor.getString("tracking_visitor_id"));
        assertNotNull(actor.getString("tracking_visit_id"));

        JSONObject eventParams = events.getJSONObject("event_params");
        assertEquals("mobile:otc:switchback:cancel:v1", eventParams.getString("pgrp"));
        assertEquals("mobile", eventParams.getString("sv"));
        assertEquals("installation-guid", eventParams.getString("dsid"));
        assertEquals("im", eventParams.getString("e"));
        assertEquals("application-name|" + BuildConfig.VERSION_NAME + "|null",
                eventParams.getString("apid"));
        assertEquals(Integer.toString(new GregorianCalendar().getTimeZone().getRawOffset() / 1000 / 60),
                eventParams.getString("g"));
        assertEquals(DeviceInspector.getDeviceName(), eventParams.getString("mdvs"));
        assertEquals("Android:sandbox:", eventParams.getString("vers"));
        assertEquals("otc", eventParams.getString("bchn"));
        assertEquals(BuildConfig.VERSION_NAME, eventParams.getString("mapv"));
        assertNotNull(eventParams.getString("vid"));
        assertEquals("out", eventParams.getString("lgin"));
        assertEquals("otc", eventParams.getString("srce"));
        assertNotNull(eventParams.getString("t"));
        assertEquals(DeviceInspector.getOs(), eventParams.getString("mosv"));
        assertEquals("mobile", eventParams.getString("bzsr"));
        assertEquals("mobile:otc:switchback:cancel:v1:Android:sandbox:", eventParams.getString("page"));
        assertEquals(Locale.getDefault().toString(), eventParams.getString("rsta"));
    }
}
