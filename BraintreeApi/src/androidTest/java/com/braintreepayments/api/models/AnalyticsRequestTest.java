package com.braintreepayments.api.models;

import android.os.Build.VERSION;
import android.test.AndroidTestCase;

import com.braintreepayments.api.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class AnalyticsRequestTest extends AndroidTestCase {

    public void testToJsonSerializesEventCorrectly() throws JSONException {
        AnalyticsRequest analyticsRequest = new AnalyticsRequest(getContext(), "analytics_event", "TEST");

        JSONObject json = new JSONObject(analyticsRequest.toJson());

        assertEquals("analytics_event", json.getJSONArray("analytics").getJSONObject(0).getString("kind"));
    }

    public void testSendsCorrectMetaData() throws JSONException {
        AnalyticsRequest analyticsRequest = new AnalyticsRequest(getContext(), "event", "TEST");

        JSONObject json = new JSONObject(analyticsRequest.toJson()).getJSONObject("_meta");

        assertEquals("Android", json.getString("platform"));
        assertEquals(BuildConfig.VERSION_NAME, json.getString("sdkVersion"));
        assertEquals("Portrait", json.getString("userInterfaceOrientation"));
        assertEquals(Integer.toString(VERSION.SDK_INT), json.getString("platformVersion"));
        assertEquals("TEST", json.getString("integrationType"));
    }

}
