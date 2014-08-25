package com.braintreepayments.api.data;

import android.app.Activity;
import android.test.AndroidTestCase;

import org.json.JSONException;
import org.json.JSONObject;

public class BraintreeDataTest extends AndroidTestCase {

    public void testInitializesCorrectlyWithBraintreeEnvironment() {
        BraintreeData braintreeData = new BraintreeData(new Activity(), BraintreeEnvironment.QA);
        assertNotNull(braintreeData);
    }

    public void testInitializesCorrectlyWithCustomMerchantIdAndUrl() {
        BraintreeData braintreeData = new BraintreeData(new Activity(), "merchant_id", "url");
        assertNotNull(braintreeData);
    }

    public void testCollectDeviceData() throws JSONException {
        BraintreeData braintreeData = new BraintreeData(new Activity(), BraintreeEnvironment.QA);

        JSONObject json = new JSONObject(braintreeData.collectDeviceData());

        assertNotNull(json.get("device_session_id"));
        assertEquals(BraintreeEnvironment.QA.getMerchantId(), json.get("fraud_merchant_id"));
    }
}