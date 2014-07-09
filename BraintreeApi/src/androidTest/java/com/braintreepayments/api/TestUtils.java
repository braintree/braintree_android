package com.braintreepayments.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.braintree.api.ClientToken;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.internal.HttpRequest.HttpMethod;
import com.braintreepayments.api.internal.HttpRequestFactory;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.CountDownLatch;

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
            ClientToken clientToken) {
        HttpRequestFactory requestFactory = new HttpRequestFactory(context) {
            @Override
            public HttpRequest getRequest(HttpMethod method, String url) {
                return new HttpRequest(new OkHttpClient(), method, url) {
                    @Override
                    public HttpRequest execute() throws UnexpectedException {
                        throw new UnexpectedException("Mocked HTTP request");
                    }
                };
            }
        };

        return new BraintreeApi(context, clientToken, requestFactory);
    }

    public static BraintreeApi apiWithExpectedResponse(Context context,
            ClientToken clientToken, final String response, final int statusCode) {
        HttpRequestFactory requestFactory = new HttpRequestFactory(context) {
            @Override
            public HttpRequest getRequest(HttpMethod method, String url) {
                return new HttpRequest(new OkHttpClient(), method, url) {
                    @Override
                    public HttpRequest execute() {
                        return this;
                    }

                    @Override
                    public String response() {
                        return response;
                    }

                    @Override
                    public int statusCode() {
                        return statusCode;
                    }
                };
            }
        };

        return new BraintreeApi(context, clientToken, requestFactory);
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
