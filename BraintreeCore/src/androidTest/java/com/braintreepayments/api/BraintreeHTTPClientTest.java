package com.braintreepayments.api;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeHTTPClientTest {

    private CountDownLatch countDownLatch;

    @Before
    public void setup() {
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void getRequestSslCertificateSuccessfulInSandbox() throws InterruptedException, InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHTTPClient braintreeHttpClient = new BraintreeHTTPClient(authorization);

        braintreeHttpClient.get("https://api.sandbox.braintreegateway.com/", null, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request should fail");
            }

            @Override
            public void failure(Exception exception) {
                // Make sure exception is due to authorization not SSL handshake
                assertTrue(exception instanceof AuthorizationException);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getRequestSslCertificateSuccessfulInProduction() throws InterruptedException, InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY);
        BraintreeHTTPClient braintreeHttpClient = new BraintreeHTTPClient(authorization);

        braintreeHttpClient.get("https://api.braintreegateway.com/", null, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request should fail");
            }

            @Override
            public void failure(Exception exception) {
                // Make sure exception is due to authorization not SSL handshake
                assertTrue(exception instanceof AuthorizationException);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
