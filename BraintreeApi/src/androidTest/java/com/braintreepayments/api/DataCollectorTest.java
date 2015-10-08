package com.braintreepayments.api;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getMockFragment;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class DataCollectorTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    @Test(timeout = 1000)
    @SmallTest
    public void getDeviceCollectorUrl_returnsCorrectUrl() {
        assertEquals("https://assets.braintreegateway.com/data/logo.htm", DataCollector.getDeviceCollectorUrl("production"));
        assertEquals("https://assets.braintreegateway.com/sandbox/data/logo.htm", DataCollector.getDeviceCollectorUrl("sandbox"));
        assertEquals("https://assets.braintreegateway.com/sandbox/data/logo.htm", DataCollector.getDeviceCollectorUrl(""));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void collectDeviceData() throws JSONException {
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(),
                mock(Configuration.class));

        String deviceData = DataCollector.collectDeviceData(fragment);

        JSONObject json = new JSONObject(deviceData);
        assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
        assertEquals("600000", json.getString("fraud_merchant_id"));
        assertNotNull(json.getString("correlation_id"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void collectDeviceData_usesDirectMerchantId() throws JSONException {
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(),
                mock(Configuration.class));

        String deviceData = DataCollector.collectDeviceData(fragment, "100");

        JSONObject json = new JSONObject(deviceData);
        assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
        assertEquals("100", json.getString("fraud_merchant_id"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void getPayPalClientMetadataId_returnsClientMetadataId() {
        BraintreeFragment fragment = getMockFragment(mActivityTestRule.getActivity(),
                mock(Configuration.class));

        String clientMetadataId = DataCollector.getPayPalClientMetadataId(
                fragment.getActivity().getApplicationContext());

        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }
}
