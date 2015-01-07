package com.braintreepayments.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpResponse;
import com.braintreepayments.testutils.FixturesHelper;

import java.util.concurrent.CountDownLatch;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static void setUp(Context context) {
        HttpRequest.DEBUG = true;
        System.setProperty("dexmaker.dexcache", context.getCacheDir().getPath());
    }

    public static ClientToken clientTokenFromFixture(Context context,
            String clientTokenFixtureFile) {
        String json = FixturesHelper.stringFromFixture(context, clientTokenFixtureFile);
        return ClientToken.getClientToken(json);
    }

    public static BraintreeApi unexpectedExceptionThrowingApi(final Context context,
            ClientToken clientToken) throws UnexpectedException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.get(anyString())).thenThrow(new UnexpectedException("Mocked HTTP request"));
        when(mockRequest.post(anyString(), anyString())).thenThrow(new UnexpectedException("Mocked HTTP request"));

        return new BraintreeApi(context, clientToken, mockRequest);
    }

    public static BraintreeApi apiWithExpectedResponse(Context context,
            ClientToken clientToken, final String response, final int statusCode)
            throws UnexpectedException {
        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.get(anyString())).thenReturn(new HttpResponse(statusCode, response));
        when(mockRequest.post(anyString(), anyString())).thenReturn(new HttpResponse(statusCode, response));

        return new BraintreeApi(context, clientToken, mockRequest);
    }

    public static void waitForMainThreadToFinish() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        latch.await();
    }

}
