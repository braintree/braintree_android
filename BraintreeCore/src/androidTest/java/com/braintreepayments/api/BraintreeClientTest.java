package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeClientTest {

    @Rule
    public final BraintreeActivityTestRule<TestActivity> mActivityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private TestActivity activity;
    private CountDownLatch countDownLatch;

    @Before
    public void setUp() {
        activity = mActivityTestRule.getActivity();
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void getConfiguration_succeedsWithATokenizationKey() throws InterruptedException {
        BraintreeClient sut = new BraintreeClient(Fixtures.TOKENIZATION_KEY, activity);

        sut.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                assertNotNull(configuration);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getConfiguration_succeedsWithAClientToken() throws InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeClient sut = new BraintreeClient(clientToken, activity);

        sut.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                assertNotNull(configuration);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Ignore("PayPalUAT development still in progress.")
    @Test(timeout = 10000)
    public void fetchConfiguration_succeedsWithAPayPalUAT() throws InterruptedException {
        String payPalUAT = new TestPayPalUATBuilder().build();
        BraintreeClient sut = new BraintreeClient(payPalUAT, activity);

        sut.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                assertNotNull(configuration);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
