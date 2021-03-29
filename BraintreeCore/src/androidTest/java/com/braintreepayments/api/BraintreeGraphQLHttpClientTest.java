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
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeGraphQLHttpClientTest {

    private CountDownLatch mCountDownLatch;

    @Before
    public void setup() {
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 5000)
    public void postRequestSslCertificateSuccessfulInSandbox() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeClient braintreeClient = new BraintreeClient(Fixtures.TOKENIZATION_KEY, context);

        braintreeClient.sendGraphQLPOST("{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                // Make sure SSL handshake is successful
                assertFalse(exception instanceof SSLException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    public void postRequestSslCertificateSuccessfulInProduction() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeClient braintreeClient = new BraintreeClient(Fixtures.PROD_TOKENIZATION_KEY, context);

        braintreeClient.sendGraphQLPOST("{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                // Make sure SSL handshake is successful
                assertFalse(exception instanceof SSLException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }
}
