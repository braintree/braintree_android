package com.braintreepayments.api;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
        HttpClient sut = new HttpClient(null, new BaseHTTPResponseParser());

        HttpRequest httpRequest = new HttpRequest()
                .method("GET")
                .baseUrl("https://bad.endpoint")
                .path("bad/path");

        sut.sendRequest(httpRequest, new HttpResponseCallback() {
            @Override
            public void onResult(String responseBody, Exception httpError) {
                assertNull(responseBody);
                assertNotNull(httpError);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
