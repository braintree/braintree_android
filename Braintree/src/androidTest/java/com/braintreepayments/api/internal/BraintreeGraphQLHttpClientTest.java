package com.braintreepayments.api.internal;

import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.test.EnvironmentHelper;
import com.braintreepayments.testutils.Assumptions;
import com.braintreepayments.testutils.FixturesHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLException;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BraintreeGraphQLHttpClientTest {

    private CountDownLatch mCountDownLatch;

    @Before
    public void setup() throws Exception {
        Assumptions.assumeDeviceCanConnectToBraintreeApi();

        mCountDownLatch = new CountDownLatch(1);
    }

    @Test
    public void sendsUserAgent() throws IOException, InvalidArgumentException {
        String baseUrl = "http://example.com/graphql";
        BraintreeGraphQLHttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);

        HttpURLConnection connection = httpClient.init(baseUrl);

        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME, connection.getRequestProperty("User-Agent"));
    }

    @Test
    public void sendsTokenizationKeyAsAuthorization() throws IOException, InvalidArgumentException {
        String baseUrl = "http://example.com/graphql";
        BraintreeGraphQLHttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);

        HttpURLConnection connection = httpClient.init(baseUrl);

        assertEquals("Bearer " + TOKENIZATION_KEY, connection.getRequestProperty("Authorization"));
    }

    @Test
    public void sendsAuthorizationFingerprintAsAuthorization() throws IOException, InvalidArgumentException {
        String baseUrl = "http://example.com/graphql";
        ClientToken clientToken = (ClientToken) Authorization.fromString(stringFromFixture("client_token.json"));
        BraintreeGraphQLHttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, clientToken.getBearer());

        HttpURLConnection connection = httpClient.init(baseUrl);

        assertEquals("Bearer " + clientToken.getAuthorizationFingerprint(), connection.getRequestProperty("Authorization"));
    }

    @Test
    public void sendsBraintreeVersionHeader() throws IOException, InvalidArgumentException {
        String baseUrl = "http://example.com/graphql";
        BraintreeGraphQLHttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);

        HttpURLConnection connection = httpClient.init(baseUrl);

        assertEquals("2018-03-06", connection.getRequestProperty("Braintree-Version"));
    }

    @Test(timeout = 5000)
    public void getRequestSslCertificateSuccessfulInSandbox() throws InterruptedException, InvalidArgumentException {
        BraintreeGraphQLHttpClient httpClient =
                new BraintreeGraphQLHttpClient("https://payments.sandbox.braintree-api.com/graphql", TOKENIZATION_KEY);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                assertFalse(exception instanceof SSLException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    public void getRequestSslCertificateSuccessfulInProduction() throws InterruptedException, InvalidArgumentException {
        BraintreeGraphQLHttpClient httpClient =
                new BraintreeGraphQLHttpClient("https://payments.braintree-api.com/graphql", TOKENIZATION_KEY);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                mCountDownLatch.countDown();
            }

            @Override
            public void failure(Exception exception) {
                assertFalse(exception instanceof SSLException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    public void getRequestBadCertificateCheck() throws InterruptedException, InvalidArgumentException {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        String baseUrl = "https://" + EnvironmentHelper.getLocalhostIp() + ":9443";
        BraintreeGraphQLHttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
                mCountDownLatch.countDown();
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

    @Test
    public void parseResponseReturnsSuccessBody() throws Exception {
        String baseUrl = "http://example.com/graphql";
        HttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);
        httpClient = HttpClientTestUtils.stubResponse(httpClient,
                200,
                FixturesHelper.stringFromFixture("response/graphql/credit_card.json"));
        HttpURLConnection connection = httpClient.init(baseUrl);

        String response = httpClient.parseResponse(connection);
        assertTrue(response.contains("tokenizeCreditCard"));
        assertFalse(response.contains("errors"));
    }

    @Test
    public void parseResponseFailsWithUserErrors() throws Exception {
        String baseUrl = "http://example.com/graphql";
        HttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);
        httpClient = HttpClientTestUtils.stubResponse(httpClient,
                200,
                FixturesHelper.stringFromFixture("errors/graphql/credit_card_error.json"));
        HttpURLConnection connection = httpClient.init(baseUrl);

        try {
            httpClient.parseResponse(connection);
            fail("No exception was thrown");
        } catch (ErrorWithResponse e) {
            assertEquals("Input is invalid.", e.getMessage());
            assertNotNull(e.errorFor("creditCard"));
        }
    }

    @Test
    public void parseResponseFailsWithValidationNotAllowed() throws Exception {
        String baseUrl = "http://example.com/graphql";
        HttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);
        httpClient = HttpClientTestUtils.stubResponse(httpClient,
                200,
                FixturesHelper.stringFromFixture("errors/graphql/validation_not_allowed_error.json"));
        HttpURLConnection connection = httpClient.init(baseUrl);

        try {
            httpClient.parseResponse(connection);
            fail("No exception was thrown");
        } catch (AuthorizationException e) {
            assertEquals("Validation is not supported for requests authorized with a tokenization key.",
                    e.getMessage());
        }
    }

    @Test
    public void parseResponseFailsWithCoercionError() throws Exception {
        String baseUrl = "http://example.com/graphql";
        HttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);
        httpClient = HttpClientTestUtils.stubResponse(httpClient,
                200,
                FixturesHelper.stringFromFixture("errors/graphql/coercion_error.json"));
        HttpURLConnection connection = httpClient.init(baseUrl);

        try {
            httpClient.parseResponse(connection);
            fail("No exception was thrown");
        } catch (UnexpectedException e) {
            assertEquals("Variable 'input' has coerced Null value for NonNull type 'String!'", e.getMessage());
        }
    }

    @Test
    public void parseResponseFailsWithUnknownError() throws Exception {
        String baseUrl = "http://example.com/graphql";
        HttpClient httpClient = new BraintreeGraphQLHttpClient(baseUrl, TOKENIZATION_KEY);
        httpClient = HttpClientTestUtils.stubResponse(httpClient,
                200,
                FixturesHelper.stringFromFixture("errors/graphql/unknown_error.json"));
        HttpURLConnection connection = httpClient.init(baseUrl);

        try {
            httpClient.parseResponse(connection);
            fail("No exception was thrown");
        } catch (UnexpectedException e) {
            assertEquals("An Unexpected Exception Occurred", e.getMessage());
        }
    }
}
