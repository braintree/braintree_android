package com.braintreepayments.api.models;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings.Secure;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AnalyticsRequestTest {

    @Test(timeout = 1000)
    @SmallTest
    public void newRequest_serializesEventCorrectly() throws JSONException {
        String analyticsRequest = AnalyticsRequest.newRequest(getTargetContext(), "analytics_event",
                "TEST");

        JSONObject json = new JSONObject(analyticsRequest);

        assertEquals("TEST.android.analytics_event", json.getJSONArray("analytics").getJSONObject(0).getString("kind"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void newRequest_sendsCorrectMetaData() throws JSONException {
        String analyticsRequest = AnalyticsRequest.newRequest(getTargetContext(), "event", "TEST");
        String uuid = getTargetContext().getSharedPreferences("BraintreeApi", Context.MODE_PRIVATE)
                .getString("braintreeUUID", null);

        JSONObject json = new JSONObject(analyticsRequest).getJSONObject("_meta");

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
        assertEquals("TEST", json.getString("integrationType"));
    }
}
