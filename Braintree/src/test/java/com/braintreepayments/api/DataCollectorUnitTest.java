package com.braintreepayments.api;

import android.text.TextUtils;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.RobolectricCountDownLatch;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestKountConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

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

    @Test(timeout = 20000)
    public void collectDeviceData_withListener() {
        Configuration configuration = new TestConfigurationBuilder()
                .kount(new TestKountConfigurationBuilder()
                        .enabled(true)
                        .kountMerchantId("500000"))
                .buildConfiguration();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();
        final RobolectricCountDownLatch latch = new RobolectricCountDownLatch(1);

        DataCollector.collectDeviceData(fragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
                try {
                    JSONObject json = new JSONObject(deviceData);
                    assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
                    assertEquals("500000", json.getString("fraud_merchant_id"));
                    assertNotNull(json.getString("correlation_id"));
                } catch (JSONException e) {
                    fail(e.getMessage());
                }

                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 20000)
    public void collectDeviceData_withListener_usesDirectMerchantId() {
        Configuration configuration = new TestConfigurationBuilder()
                .kount(new TestKountConfigurationBuilder()
                        .enabled(true)
                        .kountMerchantId("600000"))
                .buildConfiguration();
        BraintreeFragment fragment = new MockFragmentBuilder()
                .configuration(configuration)
                .build();
        final RobolectricCountDownLatch latch = new RobolectricCountDownLatch(1);

        DataCollector.collectDeviceData(fragment, "600001", new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
                try {
                    JSONObject json = new JSONObject(deviceData);
                    assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
                    assertEquals("600001", json.getString("fraud_merchant_id"));
                    assertNotNull(json.getString("correlation_id"));
                } catch (JSONException e) {
                    fail(e.getMessage());
                }

                latch.countDown();
            }
        });

        latch.await();
    }

    @Test(timeout = 5000)
    public void collectDeviceData_doesNotCollectKountDataIfKountDisabledInConfiguration() throws InterruptedException {
        BraintreeFragment fragment = new MockFragmentBuilder().build();
        final CountDownLatch latch = new CountDownLatch(1);

        DataCollector.collectDeviceData(fragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
                try {
                    JSONObject json = new JSONObject(deviceData);
                    assertNull(json.optString("device_session_id", null));
                    assertNull(json.optString("fraud_merchant_id", null));
                    assertNotNull(json.getString("correlation_id"));
                } catch (JSONException e) {
                    fail(e.getMessage());
                }

                latch.countDown();
            }
        });

        latch.await();
    }
}
