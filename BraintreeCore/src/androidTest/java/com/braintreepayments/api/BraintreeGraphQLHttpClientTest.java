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
    public void postRequestSslCertificateSuccessfulInSandbox() throws InterruptedException, InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeClient braintreeClient = new BraintreeClient(context, authorization);

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
    public void postRequestSslCertificateSuccessfulInProduction() throws InterruptedException, InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY);
        Context context = ApplicationProvider.getApplicationContext();
        BraintreeClient braintreeClient = new BraintreeClient(context, authorization);

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
