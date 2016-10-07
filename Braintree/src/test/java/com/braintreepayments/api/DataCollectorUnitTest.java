package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricGradleTestRunner.class)
public class DataCollectorUnitTest {

    @Test
    public void getDeviceCollectorEnvironment_returnsCorrectEnvironment() {
        assertEquals(com.kount.api.DataCollector.ENVIRONMENT_PRODUCTION,
                DataCollector.getDeviceCollectorEnvironment("production"));
        assertEquals(com.kount.api.DataCollector.ENVIRONMENT_TEST,
                DataCollector.getDeviceCollectorEnvironment("sandbox"));
    }

    @Test
    public void collectDeviceData() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder().build();

        String deviceData = DataCollector.collectDeviceData(fragment);

        JSONObject json = new JSONObject(deviceData);
        assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
        assertEquals("600000", json.getString("fraud_merchant_id"));
        assertNotNull(json.getString("correlation_id"));
    }

    @Test
    public void collectDeviceData_usesDirectMerchantId() throws JSONException {
        BraintreeFragment fragment = new MockFragmentBuilder().build();

        String deviceData = DataCollector.collectDeviceData(fragment, "100");

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
