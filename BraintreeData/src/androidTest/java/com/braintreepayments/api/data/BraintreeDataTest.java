package com.braintreepayments.api.data;

import android.app.Activity;
import android.test.AndroidTestCase;
import android.text.TextUtils;

import com.braintreepayments.api.data.BraintreeData;
import com.braintreepayments.api.data.BraintreeEnvironment;

public class BraintreeDataTest extends AndroidTestCase {

    public void testInitializesCorrectlyWithBraintreeEnvironment() {
        BraintreeData braintreeData = new BraintreeData(new Activity(), BraintreeEnvironment.QA);
        assertNotNull(braintreeData);
    }

    public void testInitializesCorrectlyWithCustomMerchantIdAndUrl() {
        BraintreeData braintreeData = new BraintreeData(new Activity(), "merchant_id", "url");
        assertNotNull(braintreeData);
    }

    public void testCollectDeviceDataReturnsSessionId() {
        BraintreeData braintreeData = new BraintreeData(new Activity(), BraintreeEnvironment.QA);
        assertFalse(TextUtils.isEmpty(braintreeData.collectDeviceData()));
    }
}