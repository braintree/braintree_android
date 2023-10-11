package com.braintreepayments.api;

import static junit.framework.Assert.assertNotNull;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeClientTest {

    private Context context;

    private CountDownLatch countDownLatch;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void getConfiguration_succeedsWithATokenizationKey() throws InterruptedException {
        BraintreeClient sut = new BraintreeClient(context, Fixtures.TOKENIZATION_KEY);

        sut.getConfiguration((configuration, error) -> {
            assertNotNull(configuration);
            countDownLatch.countDown();
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getConfiguration_succeedsWithAClientToken() throws InterruptedException {
        String clientToken = new TestClientTokenBuilder().build();
        BraintreeClient sut = new BraintreeClient(context, clientToken);

        sut.getConfiguration((configuration, error) -> {
            assertNotNull(configuration);
            countDownLatch.countDown();
        });

        countDownLatch.await();
    }
}
