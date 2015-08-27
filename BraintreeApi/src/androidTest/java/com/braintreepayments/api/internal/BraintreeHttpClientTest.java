package com.braintreepayments.api.internal;

import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.testutils.EnvironmentHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.api.internal.BraintreeHttpClientTestUtils.clientWithExpectedException;
import static com.braintreepayments.api.internal.BraintreeHttpClientTestUtils.clientWithExpectedResponse;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BraintreeHttpClientTest {

    private CountDownLatch mCountDownLatch;

    @Before
    public void setup() throws Exception {
        BraintreeHttpClient.DEBUG = true;
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sendsClientKeyWhenPresent() throws IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        httpClient.setClientKey("client_key");

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals("client_key", connection.getRequestProperty("Client-Key"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void doesNotSendClientKeyWhenNotPresent() throws IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertNull(connection.getRequestProperty("Client-Key"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void get_includesAuthorizationFingerprintWhenPresent()
            throws IOException, InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient() {
            @Override
            protected HttpURLConnection init(String url) throws IOException {
                assertTrue(url.contains("auth-fingerprint"));
                mCountDownLatch.countDown();

                return super.init(url);
            }
        };
        httpClient.setAuthorizationFingerprint("auth-fingerprint");

        httpClient.get("/", null);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void get_doesNotIncludeAuthorizationFingerprintWhenNotPresent()
            throws IOException, InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient() {
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
    @SmallTest
    public void sendsUserAgent() throws IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME,
                connection.getRequestProperty("User-Agent"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sendsAcceptLanguageHeader() throws IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals(Locale.getDefault().getLanguage(),
                connection.getRequestProperty("Accept-Language"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void sendsContentType() throws IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals("application/json", connection.getRequestProperty("Content-Type"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void setsDefaultConnectTimeoutOf30Seconds() throws IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals(30000, connection.getConnectTimeout());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void setDefaultReadTimeoutOf60Seconds() throws IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com/");

        assertEquals(60000, connection.getReadTimeout());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsErrorWhenBaseUrlIsNotSet() throws InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals("Protocol not found: null/", exception.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsErrorWhenURLIsNull() throws InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        httpClient.setBaseUrl(null);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals("Protocol not found: /",
                        exception.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsErrorWhenURLIsEmpty() throws InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        httpClient.setBaseUrl("");

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals("Protocol not found: /",
                        exception.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void successCallbacksHappenOnMainThread()
            throws IOException, ErrorWithResponse, InterruptedException {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(200, "");
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                countDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                fail("Request failed");
            }
        });

        httpClient.post("/", "{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                countDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                fail("Request failed");
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void failureCallbacksHappenOnMainThread() throws InterruptedException, IOException {
        BraintreeHttpClient httpClient = clientWithExpectedException(new IOException());
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
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
                assertEquals(Looper.getMainLooper(), Looper.myLooper());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsIOExceptionWhenHttpRequestBlowsUp() throws IOException, InterruptedException {
        BraintreeHttpClient httpClient = clientWithExpectedException(new IOException());

        assertExceptionIsThrown(httpClient, IOException.class, null);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsServerErrorWhenServerReturns500() throws IOException, InterruptedException,
            ErrorWithResponse {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(500, "");

        assertExceptionIsThrown(httpClient, ServerException.class, null);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsDownForMaintenanceWhenServerIsDown()
            throws IOException, InterruptedException, ErrorWithResponse {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(503, "");

        assertExceptionIsThrown(httpClient, DownForMaintenanceException.class, null);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsUpgradeRequiredExceptionOn426()
            throws IOException, InterruptedException, ErrorWithResponse {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(426, "");

        assertExceptionIsThrown(httpClient, UpgradeRequiredException.class, null);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsAuthenticationExceptionOn401()
            throws IOException, InterruptedException, ErrorWithResponse {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(401, "");

        assertExceptionIsThrown(httpClient, AuthenticationException.class, null);
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsAuthorizationExceptionOn403()
            throws IOException, InterruptedException, ErrorWithResponse {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(403, stringFromFixture("error_response.json"));

        assertExceptionIsThrown(httpClient, AuthorizationException.class, "There was an error");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsErrorWithResponseOn422()
            throws IOException, InterruptedException, ErrorWithResponse {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(422,
                stringFromFixture("error_response.json"));

        assertExceptionIsThrown(httpClient, ErrorWithResponse.class, "There was an error");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void throwsUnknownExceptionOnUnrecognizedStatusCode()
            throws IOException, InterruptedException, ErrorWithResponse {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(418, "");

        assertExceptionIsThrown(httpClient, UnexpectedException.class, null);
    }

    @Test(timeout = 5000)
    @MediumTest
    public void getRequestSslCertificateSuccessfulInSandbox() throws InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        httpClient.setBaseUrl("https://api.sandbox.braintreegateway.com");

        httpClient.get("/wellness", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                assertNotNull(responseBody);
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                fail("Request failed");
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    @MediumTest
    public void getRequestSslCertificateSuccessfulInProduction() throws InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        httpClient.setBaseUrl("https://api.braintreegateway.com");

        httpClient.get("/wellness", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                assertNotNull(responseBody);
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                fail("Request failed");
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    @MediumTest
    public void getRequestBadCertificateCheck() throws InterruptedException {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        BraintreeHttpClient httpClient = new BraintreeHttpClient();
        httpClient.setBaseUrl("https://" + EnvironmentHelper.getLocalhostIp() + ":9443");

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(
                        "java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.",
                        exception.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    /* helpers */
    private void assertExceptionIsThrown(BraintreeHttpClient httpClient,
            final Class<? extends Exception> exceptionType, final String exceptionMessage)
            throws IOException, InterruptedException {
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
