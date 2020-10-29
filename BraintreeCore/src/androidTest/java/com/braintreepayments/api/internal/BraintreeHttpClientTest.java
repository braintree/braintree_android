package com.braintreepayments.api.internal;

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.internal.HttpClientTestUtils.stubResponse;
import static com.braintreepayments.testutils.FixturesHelper.base64Encode;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class BraintreeHttpClientTest {

    private CountDownLatch mCountDownLatch;

    @Before
    public void setup() {
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 1000)
    public void getUserAgent_returnsCorrectUserAgent() {
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME,
                BraintreeHttpClient.getUserAgent());
    }

    @Test(timeout = 1000)
    public void sendsUserAgent() throws IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME,
                connection.getRequestProperty("User-Agent"));
    }

    @Test(timeout = 1000)
    public void sendsTokenizationKeyWhenPresent() throws IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals(Fixtures.TOKENIZATION_KEY, connection.getRequestProperty("Client-Key"));
    }

    @Test(timeout = 1000)
    public void doesNotSendTokenizationKeyWhenNotPresent()
            throws IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                Authorization
                        .fromString(base64Encode(Fixtures.CLIENT_TOKEN)));

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertNull(connection.getRequestProperty("Client-Key"));
    }

    @Test(timeout = 1000)
    public void get_includesAuthorizationFingerprintWhenPresent()
            throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(Authorization
                .fromString(base64Encode(Fixtures.CLIENT_TOKEN))) {
            @Override
            protected HttpURLConnection init(String url) throws IOException {
                assertTrue(url.contains("authorization_fingerprint"));
                mCountDownLatch.countDown();

                return super.init(url);
            }
        };

        httpClient.get("/", null);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void get_doesNotIncludeAuthorizationFingerprintWhenNotPresent()
            throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY)) {
            @Override
            protected HttpURLConnection init(String url) throws IOException {
                assertFalse(url.contains("authorizationFingerprint"));
                mCountDownLatch.countDown();

                return super.init(url);
            }
        };

        httpClient.get("/", null);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void postsErrorWhenBaseUrlIsNotSet()
            throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(MalformedURLException.class, exception.getClass());
                countDownLatch.countDown();
            }
        });

        httpClient.post("/", "{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(MalformedURLException.class, exception.getClass());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 1000)
    public void postsErrorWhenBaseUrlIsNull() throws Exception {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));
        httpClient.setBaseUrl(null);

        assertExceptionIsPosted(httpClient, MalformedURLException.class, null);
    }


    @Test(timeout = 1000)
    public void postsErrorWhenBaseUrlIsEmpty() throws Exception {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));
        httpClient.setBaseUrl("");

        assertExceptionIsPosted(httpClient, MalformedURLException.class, null);
    }

    @Test(timeout = 1000)
    public void postsErrorWhenPathIsNull() throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get(null, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(IllegalArgumentException.class, exception.getClass());
                assertEquals("Path cannot be null", exception.getMessage());
                countDownLatch.countDown();
            }
        });

        httpClient.post(null, "{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(IllegalArgumentException.class, exception.getClass());
                assertEquals("Path cannot be null", exception.getMessage());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 1000)
    public void postsErrorWhenClientTokenIsUsedAndInvalidJsonIsSent()
            throws InvalidArgumentException, InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                Authorization
                        .fromString(base64Encode(Fixtures.CLIENT_TOKEN)));

        httpClient.post("/", "not json", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertTrue(exception instanceof JSONException);
                assertEquals("Value not of type java.lang.String cannot be converted to JSONObject",
                        exception.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void throwsAuthorizationExceptionWithCorrectMessageOn403() throws Exception {
        assertExceptionIsPosted(403, Fixtures.ERROR_RESPONSE,AuthorizationException.class,
                "There was an error");
    }

    @Test(timeout = 1000)
    public void throwsErrorWithResponseOn422() throws Exception {
        assertExceptionIsPosted(422, Fixtures.ERROR_RESPONSE, ErrorWithResponse.class,
                "There was an error");
    }

    @Test(timeout = 5000)
    public void getRequestSslCertificateSuccessfulInSandbox() throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));
        httpClient.setBaseUrl("https://api.sandbox.braintreegateway.com");

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request failed");
            }

            @Override
            public void failure(Exception exception) {
                assertTrue(exception instanceof AuthorizationException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 10000)
    public void getRequestSslCertificateSuccessfulInProduction() throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(Fixtures.TOKENIZATION_KEY));
        httpClient.setBaseUrl("https://api.braintreegateway.com");

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request failed");
            }

            @Override
            public void failure(Exception exception) {
                assertTrue(exception instanceof AuthorizationException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    /* helpers */
    private void assertExceptionIsPosted(int responseCode, String responseBody,
            final Class<? extends Exception> exceptionType, final String exceptionMessage) throws Exception {
        assertExceptionIsPosted((BraintreeHttpClient) stubResponse(new BraintreeHttpClient(null), responseCode, responseBody),
                exceptionType, exceptionMessage);
    }

    private void assertExceptionIsPosted(BraintreeHttpClient httpClient,
            final Class<? extends Exception> exceptionType, final String exceptionMessage) throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(exceptionType, exception.getClass());
                if (exceptionMessage != null) {
                    assertEquals(exceptionMessage, exception.getMessage());
                }
                countDownLatch.countDown();
            }
        });

        httpClient.post("/", "{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(exceptionType, exception.getClass());
                if (exceptionMessage != null) {
                    assertEquals(exceptionMessage, exception.getMessage());
                }
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }
}
