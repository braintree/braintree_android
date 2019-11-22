package com.braintreepayments.api;

import android.text.TextUtils;

import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.testutils.TestConfigurationBuilder;
import com.braintreepayments.testutils.TestConfigurationBuilder.TestKountConfigurationBuilder;
import com.kount.api.DataCollector.CompletionHandler;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({ com.kount.api.DataCollector.class })
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

        mockKount(true);
    }

    @Test
    public void getDeviceCollectorEnvironment_returnsCorrectEnvironment() {
        assertEquals(com.kount.api.DataCollector.ENVIRONMENT_PRODUCTION,
                DataCollector.getDeviceCollectorEnvironment("production"));
        assertEquals(com.kount.api.DataCollector.ENVIRONMENT_TEST,
                DataCollector.getDeviceCollectorEnvironment("sandbox"));
    }

    @Test
    public void collectDeviceData() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
                try {
                    JSONObject json = new JSONObject(deviceData);
                    assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
                    assertEquals("600000", json.getString("fraud_merchant_id"));
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
    public void collectDeviceData_usesDirectMerchantId() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        DataCollector.collectDeviceData(mBraintreeFragment, "100", new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String deviceData) {
                try {
                    JSONObject json = new JSONObject(deviceData);
                    assertFalse(TextUtils.isEmpty(json.getString("device_session_id")));
                    assertEquals("100", json.getString("fraud_merchant_id"));
                    latch.countDown();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        latch.await();
    }

    @Test
    public void collectDeviceData_sendsAnalyticsEventsWhenSuccessful() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String s) {
                verify(mBraintreeFragment).sendAnalyticsEvent("data-collector.kount.started");
                verify(mBraintreeFragment).sendAnalyticsEvent("data-collector.kount.succeeded");

                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    public void collectDeviceData_sendsAnalyticsEventsWhenFailed() throws InterruptedException {
        mockKount(false);
        final CountDownLatch latch = new CountDownLatch(1);

        DataCollector.collectDeviceData(mBraintreeFragment, new BraintreeResponseListener<String>() {
            @Override
            public void onResponse(String s) {
                verify(mBraintreeFragment).sendAnalyticsEvent("data-collector.kount.started");
                verify(mBraintreeFragment).sendAnalyticsEvent("data-collector.kount.failed");

                latch.countDown();
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

    private void mockKount(final boolean kountIsSuccessful) {
        final com.kount.api.DataCollector mockDataCollector = mock(com.kount.api.DataCollector.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                CompletionHandler handler = ((CompletionHandler) invocation.getArguments()[1]);
                String id = (String) invocation.getArguments()[0];

                if (kountIsSuccessful) {
                    handler.completed(id);
                } else {
                    handler.failed(id, null);

                }
                return null;
            }
        }).when(mockDataCollector).collectForSession(anyString(), any(CompletionHandler.class));

        mockStatic(com.kount.api.DataCollector.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return mockDataCollector;
            }
        }).when(com.kount.api.DataCollector.class);
        com.kount.api.DataCollector.getInstance();
    }
}
