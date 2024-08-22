package com.braintreepayments.api.sharedutils;

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
                .method(HttpMethod.GET)
                .baseUrl("https://bad.endpoint")
                .path("bad/path");

        sut.sendRequest(httpRequest, (responseBody, httpError) -> {
            assertNull(responseBody);
            assertNotNull(httpError);
            countDownLatch.countDown();
        });

        countDownLatch.await();
    }
}
