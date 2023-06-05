package com.braintreepayments.api;

import android.text.TextUtils;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestKountConfigurationBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "androidx.*" })
public class DataCollectorUnitTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();
    private BraintreeFragment mBraintreeFragment;

    @Before
    public void setup() {
        mBraintreeFragment = new MockFragmentBuilder()
                .configuration(new TestConfigurationBuilder()
                        .kount(new TestKountConfigurationBuilder()
                                .kountMerchantId("600000"))
                        .build())
                .build();
    }

    @Test
    public void collectDeviceData() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
                try {
                    JSONObject json = new JSONObject(deviceData);
                    assertNotNull(json.getString("correlation_id"));
                    latch.countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        latch.await();

    }

    @Test
    public void getPayPalClientMetadataId_returnsClientMetadataId() {
        String clientMetadataId = DataCollector.getPayPalClientMetadataId(RuntimeEnvironment.application);
        assertFalse(TextUtils.isEmpty(clientMetadataId));
    }

    @Test
    public void collectPayPalDeviceData() throws InterruptedException {
        final BraintreeFragment fragment = new MockFragmentBuilder().build();
        final CountDownLatch latch = new CountDownLatch(1);

        DataCollector.collectPayPalDeviceData(fragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    JSONObject json = new JSONObject(s);
                    assertNotNull(json.getString("correlation_id"));
                } catch (JSONException e) {
                    fail();
                }
                latch.countDown();
            }
        });
        latch.await();
    }
}
