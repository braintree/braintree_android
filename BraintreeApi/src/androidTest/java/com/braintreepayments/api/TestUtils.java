package com.braintreepayments.api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PayPalAccountBuilder;
import com.braintreepayments.testutils.FixturesHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.internal.HttpRequestTestUtils.requestWithExpectedResponse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static void setUp(Context context) {
        HttpRequest.DEBUG = true;
        System.setProperty("dexmaker.dexcache", context.getCacheDir().getPath());
    }

    public static Configuration getConfigurationFromFixture(Context context, String fixture) {
        return Configuration.fromJson(FixturesHelper.stringFromFixture(context, fixture));
    }

    public static BraintreeApi unexpectedExceptionThrowingApi(final Context context)
            throws BraintreeException, ErrorWithResponse {
        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.get(anyString())).thenThrow(new UnexpectedException("Mocked HTTP request"));
        when(mockRequest.post(anyString(), anyString())).thenThrow(new UnexpectedException("Mocked HTTP request"));

        return new BraintreeApi(context, mock(ClientToken.class), mock(Configuration.class), mockRequest);
    }

    public static BraintreeApi apiWithExpectedResponse(Context context, final int responseCode,
            final String response) throws IOException, ErrorWithResponse {
        return new BraintreeApi(context, mock(ClientToken.class), mock(Configuration.class),
                requestWithExpectedResponse(responseCode, response));
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

    public static PayPalAccountBuilder fakePayPalAccountBuilder() throws JSONException {
        return new PayPalAccountBuilder().OtcResponse(
                new JSONObject(
                        "{\"client\":{\"environment\":\"mock\",\"paypal_sdk_version\":\"1.0.8\",\"platform\":\"Android\",\"product_name\":\"OneTouchCore-Android\"},\"response\":{\"code\":\"fake_code\"},\"response_type\":\"authorization_code\",\"user\":{\"display_string\":\"scruff@mcgruff.com\"}}"));
    }
}
