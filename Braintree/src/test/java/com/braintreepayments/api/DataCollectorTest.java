package com.braintreepayments.api;

import android.text.TextUtils;

import com.braintreepayments.api.models.Configuration;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricGradleTestRunner.class)
public class DataCollectorTest {

    @Test
    public void getDeviceCollectorUrl_returnsCorrectUrl() {
        assertEquals("https://assets.braintreegateway.com/data/logo.htm", DataCollector.getDeviceCollectorUrl("production"));
        assertEquals("https://assets.braintreegateway.com/sandbox/data/logo.htm", DataCollector.getDeviceCollectorUrl("sandbox"));
        assertEquals("https://assets.braintreegateway.com/sandbox/data/logo.htm", DataCollector.getDeviceCollectorUrl(""));
    }

    @Test
    public void collectDeviceData() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mock(Configuration.class))
                .build();

        String deviceData = DataCollector.collectDeviceData(RuntimeEnvironment.application, fragment);

        JSONObject json = new JSONObject(deviceData);
        assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
        assertEquals("600000", json.getString("fraud_merchant_id"));
        assertNotNull(json.getString("correlation_id"));
    }

    @Test
    public void collectDeviceData_usesDirectMerchantId() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(mock(Configuration.class))
                .build();

        String deviceData = DataCollector.collectDeviceData(RuntimeEnvironment.application, fragment, "100");

        JSONObject json = new JSONObject(deviceData);
        assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
        assertEquals("100", json.getString("fraud_merchant_id"));
    }

    @Test
    public void getPayPalClientMetadataId_returnsClientMetadataId() {
        String clientMetadataId = DataCollector.getPayPalClientMetadataId(RuntimeEnvironment.application);

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }
}
