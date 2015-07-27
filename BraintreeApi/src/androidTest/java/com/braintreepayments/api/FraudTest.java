package com.braintreepayments.api;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.Fraud.BraintreeEnvironment;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class FraudTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    @Test(timeout = 1000)
    @SmallTest
    public void getMerchantId_returnsCorrectMerchantId() {
        assertEquals("600000", Fraud.BraintreeEnvironment.SANDBOX.getMerchantId());
        assertEquals("600000", Fraud.BraintreeEnvironment.PRODUCTION.getMerchantId());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getCollectorUrl_returnsCorrectUrl() {
        assertEquals("https://assets.braintreegateway.com/sandbox/data/logo.htm", Fraud.BraintreeEnvironment.SANDBOX.getCollectorUrl());
        assertEquals("https://assets.braintreegateway.com/data/logo.htm",
                Fraud.BraintreeEnvironment.PRODUCTION.getCollectorUrl());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void collectDeviceData() throws JSONException {
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(),
                mock(Configuration.class));

        String deviceData = Fraud.collectDeviceData(fragment);

        JSONObject json = new JSONObject(deviceData);
        assertNotNull(json.get("device_session_id"));
        assertEquals(BraintreeEnvironment.SANDBOX.getMerchantId(), json.get("fraud_merchant_id"));
        assertNotNull(json.get("correlation_id"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void collectDeviceData_usesDirectMerchantId() throws JSONException {
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(),
                mock(Configuration.class));

        String deviceData = Fraud.collectDeviceData(fragment, "100", "http://example.com");

        JSONObject json = new JSONObject(deviceData);
        assertNotNull(json.get("device_session_id"));
        assertEquals("100", json.get("fraud_merchant_id"));
    }
}
