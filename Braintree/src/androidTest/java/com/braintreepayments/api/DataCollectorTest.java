package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.test.BraintreeActivityTestRule;
import com.braintreepayments.api.test.TestActivity;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestKountConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.BraintreeFragmentTestUtils.getFragmentWithConfiguration;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class DataCollectorTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private AppCompatActivity mActivity;
    private CountDownLatch mCountDownLatch;

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void collectDeviceData_withListener() throws InterruptedException {
        String configuration = new TestConfigurationBuilder()
                .kount(new TestKountConfigurationBuilder()
                        .kountMerchantId("500000"))
                .build();
        BraintreeFragment fragment = getFragmentWithConfiguration(mActivity, configuration);

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

                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void collectDeviceData_withListener_usesDirectMerchantId() throws InterruptedException {
        String configuration = new TestConfigurationBuilder()
                .kount(new TestKountConfigurationBuilder()
                        .kountMerchantId("600000"))
                .build();
        BraintreeFragment fragment = getFragmentWithConfiguration(mActivity, configuration);

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

                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void collectDeviceData_doesNotCollectKountDataIfKountDisabledInConfiguration() throws InterruptedException {
        BraintreeFragment fragment = getFragmentWithConfiguration(mActivity, new TestConfigurationBuilder().build());

        DataCollector.collectDeviceData(fragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
                try {
                    JSONObject json = new JSONObject(deviceData);
                    assertNull(Json.optString(json, "device_session_id", null));
                    assertNull(Json.optString(json, "fraud_merchant_id", null));
                    assertNotNull(json.getString("correlation_id"));
                } catch (JSONException e) {
                    fail(e.getMessage());
                }

                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }
}
