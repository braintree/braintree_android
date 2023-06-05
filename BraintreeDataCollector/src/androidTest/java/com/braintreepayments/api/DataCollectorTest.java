package com.braintreepayments.api;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4ClassRunner.class)
public class DataCollectorTest {

    private Context context;

    private CountDownLatch countDownLatch;

    @Before
    public void beforeEach() {
        context = ApplicationProvider.getApplicationContext();
        countDownLatch = new CountDownLatch(1);
    }

    @After
    public void afterEach() {
        SharedPreferencesHelper.clearConfigurationCacheOverride(context);
    }

    @Test(timeout = 10000)
    public void collectDeviceData_returnsCorrelationIdAndDoesNotCollectKountData() throws InterruptedException, InvalidArgumentException {
        Configuration configuration = new TestConfigurationBuilder().buildConfiguration();
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);

        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration);

        BraintreeClient braintreeClient = new BraintreeClient(context, Fixtures.TOKENIZATION_KEY);
        DataCollector sut = new DataCollector(braintreeClient);

        sut.collectDeviceData(context, new DataCollectorCallback() {
            @Override
            public void onResult(@Nullable String deviceData, @Nullable Exception error) {
                try {
                    assertNotNull(deviceData);
                    JSONObject json = new JSONObject(deviceData);
                    assertNull(Json.optString(json, "device_session_id", null));
                    assertNull(Json.optString(json, "fraud_merchant_id", null));
                    assertNotNull(json.getString("correlation_id"));
                    countDownLatch.countDown();
                } catch (JSONException e) {
                    fail(e.getMessage());
                }
            }
        });

        countDownLatch.await();
    }
}
