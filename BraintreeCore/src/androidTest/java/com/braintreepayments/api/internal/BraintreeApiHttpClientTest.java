package com.braintreepayments.api.internal;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.BraintreeApiErrorResponse;
import com.braintreepayments.api.interfaces.HttpResponseCallback;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.internal.HttpClientTestUtils.stubResponse;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeApiHttpClientTest {

    @Test(timeout = 1000)
    public void setsBaseUrl() throws IOException, InterruptedException {
        BraintreeApiHttpClient client = spy(new BraintreeApiHttpClient("http://localhost", null));

        final CountDownLatch latch = new CountDownLatch(1);
        client.get("/some-path", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                latch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                latch.countDown();
            }
        });

        latch.await();

        verify(client).init(eq("http://localhost/some-path"));
    }

    @Test
    public void setsAuthorizationHeader() throws IOException {
        BraintreeApiHttpClient client = new BraintreeApiHttpClient(null, "access-token-example");

        HttpURLConnection connection = client.init("http://example.com/");

        assertEquals("Bearer access-token-example", connection.getRequestProperty("Authorization"));
    }

    @Test
    public void setsUserAgent() throws IOException {
        BraintreeApiHttpClient client = new BraintreeApiHttpClient(null, null);

        HttpURLConnection connection = client.init("http://example.com/");

        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, connection.getRequestProperty("User-Agent"));
    }

    @Test(timeout = 5000)
    public void throwsBraintreeApiErrorResponseExceptionOn400() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final Class expectedException = BraintreeApiErrorResponse.class;
        final String expectedMessage = "The provided parameters are invalid; see details for field-specific error messages.";
        BraintreeApiHttpClient client = new BraintreeApiHttpClient(null, null);
        client = (BraintreeApiHttpClient) stubResponse(client, 400,
                stringFromFixture("errors/braintree_api_error_response.json"));

        client.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(expectedException, exception.getClass());
                assertEquals(expectedMessage, exception.getMessage());
                countDownLatch.countDown();
            }
        });

        client.post("/", "{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(expectedException, exception.getClass());
                assertEquals(expectedMessage, exception.getMessage());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 5000)
    public void getRequestSslCertificateSuccessfulInSandbox() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeApiHttpClient client = new BraintreeApiHttpClient("https://payments.sandbox.braintree-api.com", null);

        client.get("/ping", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    assertEquals("OK", new JSONObject(responseBody).getJSONObject("data").getString("status"));
                } catch (JSONException e) {
                    fail("Response invalid");
                }

                latch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                fail("Request failed");
            }
        });

        latch.await();
    }

    @Test(timeout = 5000)
    public void getRequestSslCertificateSuccessfulInProduction() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeApiHttpClient client = new BraintreeApiHttpClient("https://payments.braintree-api.com", null);

        client.get("/ping", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                try {
                    assertEquals("OK", new JSONObject(responseBody).getJSONObject("data").getString("status"));
                } catch (JSONException e) {
                    fail("Response invalid");
                }

                latch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                fail("Request failed");
            }
        });

        latch.await();
    }

    @Test(timeout = 5000)
    public void getRequest_whenErrorOccurs_callsFailure() throws InterruptedException {

        final CountDownLatch latch = new CountDownLatch(1);
        BraintreeApiHttpClient client = new BraintreeApiHttpClient("https://bad.endpoint", null);

        client.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertNotNull(exception);
                latch.countDown();
            }
        });

        latch.await();
    }
}
