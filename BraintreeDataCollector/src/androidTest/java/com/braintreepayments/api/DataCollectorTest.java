package com.braintreepayments.api;

import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.TestConfigurationBuilder.TestKountConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

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
    public void beforeEach() {
        mActivity = mActivityTestRule.getActivity();
        mCountDownLatch = new CountDownLatch(1);
    }

    @After
    public void afterEach() {
        SharedPreferencesHelper.clearConfigurationCacheOverride(mActivity);
    }

    @Test(timeout = 10000)
    public void collectDeviceData_withListener() throws InterruptedException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .kount(new TestKountConfigurationBuilder().kountMerchantId("500000"))
                .buildConfiguration();
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);

        SharedPreferencesHelper.overrideConfigurationCache(mActivity, authorization, configuration);

        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, "sample-scheme");
        DataCollector sut = new DataCollector(braintreeClient);

        sut.collectDeviceData(mActivity, new DataCollectorCallback() {
            @Override
            public void onResult(@Nullable String deviceData, @Nullable Exception error) {
                try {
                    assertNotNull(deviceData);
                    JSONObject json = new JSONObject(deviceData);
                    assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
                    assertEquals("500000", json.getString("fraud_merchant_id"));
                    assertNotNull(json.getString("correlation_id"));
                    mCountDownLatch.countDown();
                } catch (JSONException e) {
                    fail(e.getMessage());
                }
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void collectDeviceData_withListener_usesDirectMerchantId() throws InterruptedException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder()
                .kount(new TestKountConfigurationBuilder()
                        .kountMerchantId("600000"))
                .buildConfiguration();

        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        SharedPreferencesHelper.overrideConfigurationCache(mActivity, authorization, configuration);

        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, "sample-scheme");
        DataCollector sut = new DataCollector(braintreeClient);

        sut.collectDeviceData(mActivity, "600001", new DataCollectorCallback() {
            @Override
            public void onResult(@Nullable String deviceData, @Nullable Exception error) {
                try {
                    assertNotNull(deviceData);
                    JSONObject json = new JSONObject(deviceData);
                    assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
                    assertEquals("600001", json.getString("fraud_merchant_id"));
                    assertNotNull(json.getString("correlation_id"));
                    mCountDownLatch.countDown();
                } catch (JSONException e) {
                    fail(e.getMessage());
                }
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void collectDeviceData_doesNotCollectKountDataIfKountDisabledInConfiguration() throws InterruptedException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);

        SharedPreferencesHelper.overrideConfigurationCache(mActivity, authorization, configuration);

        BraintreeClient braintreeClient = new BraintreeClient(authorization, mActivity, "sample-scheme");
        DataCollector sut = new DataCollector(braintreeClient);

        sut.collectDeviceData(mActivity, new DataCollectorCallback() {
            @Override
            public void onResult(@Nullable String deviceData, @Nullable Exception error) {
                try {
                    assertNotNull(deviceData);
                    JSONObject json = new JSONObject(deviceData);
                    assertNull(Json.optString(json, "device_session_id", null));
                    assertNull(Json.optString(json, "fraud_merchant_id", null));
                    assertNotNull(json.getString("correlation_id"));
                    mCountDownLatch.countDown();
                } catch (JSONException e) {
                    fail(e.getMessage());
                }
            }
        });

        mCountDownLatch.await();
    }
}
