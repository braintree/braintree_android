package com.braintreepayments.api;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.BaseHttpResponseParser;
import com.braintreepayments.api.HttpClient;
import com.braintreepayments.api.HttpRequest;
import com.braintreepayments.api.HttpResponseCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class HttpClientTest {

    private CountDownLatch countDownLatch;

    @Before
    public void beforeEach() {
        countDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 5000)
    public void sendRequest_whenErrorOccurs_callsFailure() throws InterruptedException {
        HttpClient sut = new HttpClient(null, new BaseHttpResponseParser());

        HttpRequest httpRequest = new HttpRequest()
                .method("GET")
                .baseUrl("https://bad.endpoint")
                .path("bad/path");

        sut.sendRequest(httpRequest, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("request should fail");
                countDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                assertNotNull(exception);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
