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
    public final BraintreeActivityTestRule<TestActivity> activityTestRule =
            new BraintreeActivityTestRule<>(TestActivity.class);

    private TestActivity activity;
    private CountDownLatch countDownLatch;

    @Before
    public void setUp() {
        activity = activityTestRule.getActivity();
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void getConfiguration_succeedsWithATokenizationKey() throws InvalidArgumentException, InterruptedException {
        BraintreeClient sut = new BraintreeClient(activity, Fixtures.TOKENIZATION_KEY);

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
    public void getConfiguration_succeedsWithAClientToken() throws InterruptedException, InvalidArgumentException {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeClient sut = new BraintreeClient(activity, clientToken);

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
    public void fetchConfiguration_succeedsWithAPayPalUAT() throws InterruptedException, InvalidArgumentException {
        String payPalUAT = new TestPayPalUATBuilder().build();
        BraintreeClient sut = new BraintreeClient(activity, payPalUAT);

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
