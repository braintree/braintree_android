package com.braintreepayments.api;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeHttpClientTest {

    private CountDownLatch countDownLatch;

    @Before
    public void setup() {
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 10000)
    public void getRequestSslCertificateSuccessfulInSandbox() throws InterruptedException {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        BraintreeHttpClient braintreeHttpClient = new BraintreeHttpClient();

        braintreeHttpClient.get("https://api.sandbox.braintreegateway.com/", null, authorization, new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                // Make sure exception is due to authorization not SSL handshake
                assertTrue(httpError instanceof AuthorizationException);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getRequestSslCertificateSuccessfulInQA() throws InterruptedException {
        Authorization authorization = Authorization.fromString("development_testing_integration_merchant_id");
        BraintreeHttpClient braintreeHttpClient = new BraintreeHttpClient();

        braintreeHttpClient.get("https://gateway.qa.braintreepayments.com/", null, authorization, new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                // Make sure http request to qa works to verify certificate pinning strategy
                assertNull(httpError);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getRequestSslCertificateSuccessfulInProduction() throws InterruptedException {
        Authorization authorization = Authorization.fromString(Fixtures.PROD_TOKENIZATION_KEY);
        BraintreeHttpClient braintreeHttpClient = new BraintreeHttpClient();

        braintreeHttpClient.get("https://api.braintreegateway.com/",null, authorization, new HttpResponseCallback() {

            @Override
            public void onResult(String responseBody, Exception httpError) {
                // Make sure exception is due to authorization not SSL handshake
                assertTrue(httpError instanceof AuthorizationException);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
