package com.braintreepayments.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

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

        sut.sendRequest(httpRequest, new HttpTimingResponseCallback() {
            @Override
            public void onResult(HttpResponse response, Exception httpError) {
                assertNull(response);
                assertNotNull(httpError);
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
