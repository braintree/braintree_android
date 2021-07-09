package com.braintreepayments.api;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLException;

import static junit.framework.Assert.assertFalse;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeGraphQLClientTest {

    private CountDownLatch countDownLatch;

    @Before
    public void setup() {
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 5000)
    public void postRequestSslCertificateSuccessfulInSandbox() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeClient braintreeClient = new BraintreeClient(context, Fixtures.TOKENIZATION_KEY);

        braintreeClient.sendGraphQLPOST("{}", new HttpResponseCallback() {
            @Override
            public void onResult(String responseBody, Exception httpError) {
                // Make sure SSL handshake is successful
                assertFalse(httpError instanceof SSLException);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 5000)
    public void postRequestSslCertificateSuccessfulInProduction() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeClient braintreeClient = new BraintreeClient(context, Fixtures.PROD_TOKENIZATION_KEY);

        braintreeClient.sendGraphQLPOST("{}", new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                // Make sure SSL handshake is successful
                assertFalse(httpError instanceof SSLException);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
