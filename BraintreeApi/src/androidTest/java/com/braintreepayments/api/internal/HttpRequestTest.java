package com.braintreepayments.api.internal;

import android.test.AndroidTestCase;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.testutils.EnvironmentHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Locale;

import static com.braintreepayments.api.internal.HttpRequestTestUtils.streamFromString;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HttpRequestTest extends AndroidTestCase {

    @Override
    public void setUp() {
        HttpRequest.DEBUG = true;
    }

    public void testSendsUserAgent() throws IOException {
        HttpRequest httpRequest = new HttpRequest("http://example.com", "");

        HttpURLConnection connection = httpRequest.init("http://example.com/");

        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME,
                connection.getRequestProperty("User-Agent"));
    }

    public void testSendsAcceptLanguageHeader() throws IOException {
        HttpRequest httpRequest = new HttpRequest("http://example.com", "");

        HttpURLConnection connection = httpRequest.init("http://example.com/");

        assertEquals(Locale.getDefault().getLanguage(),
                connection.getRequestProperty("Accept-Language"));
    }

    public void testSendsContentType() throws IOException {
        HttpRequest httpRequest = new HttpRequest("http://example.com", "");

        HttpURLConnection connection = httpRequest.init("http://example.com/");

        assertEquals("application/json", connection.getRequestProperty("Content-Type"));
    }

    public void testThrowsErrorWhenURLIsNull() {
        HttpRequest httpRequest = new HttpRequest(null, null);

        try {
            httpRequest.get("/").getResponseCode();
            fail("No exception was thrown");
        } catch (ErrorWithResponse errorWithResponse) {
            fail("Receive incorrect error");
        } catch (BraintreeException e) {
            assertEquals("Protocol not found: /?authorizationFingerprint=", e.getMessage());
        }
    }

    public void testThrowsErrorWhenURLIsEmpty() {
        HttpRequest httpRequest = new HttpRequest("", null);

        try {
            httpRequest.get("/").getResponseCode();
            fail("No exception was thrown");
        } catch (ErrorWithResponse errorWithResponse) {
            fail("Receive incorrect error");
        } catch (BraintreeException e) {
            assertEquals("Protocol not found: /?authorizationFingerprint=", e.getMessage());
        }
    }


    public void testThrowsUnexpectedExceptionWhenHttpRequestBlowsUp() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenThrow(new IOException());

        assertExceptionIsThrown(connection, UnexpectedException.class, null);
    }

    public void testThrowsServerErrorWhenServerReturns500() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(500);

        assertExceptionIsThrown(connection, ServerException.class, null);
    }

    public void testThrowsDownForMaintenanceWhenServerIsDown() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(503);

        assertExceptionIsThrown(connection, DownForMaintenanceException.class, null);
    }

    public void testThrowsUpgradeRequiredExceptionOn426() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(426);

        assertExceptionIsThrown(connection, UpgradeRequiredException.class, null);
    }

    public void testThrowsAuthenticationExceptionOn401() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(401);

        assertExceptionIsThrown(connection, AuthenticationException.class, null);
    }

    public void testThrowsAuthorizationExceptionOn403() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(403);

        assertExceptionIsThrown(connection, AuthorizationException.class, null);
    }

    public void testThrowsErrorWithResponseOn422() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(422);
        when(connection.getErrorStream()).thenReturn(streamFromString(stringFromFixture(getContext(), "error_response.json")))
            .thenReturn(streamFromString(stringFromFixture(getContext(), "error_response.json")));

        assertExceptionIsThrown(connection, ErrorWithResponse.class, "There was an error");
    }

    public void testThrowsUnknownExceptionOnUnrecognizedStatusCode() throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(418);

        assertExceptionIsThrown(connection, UnexpectedException.class, null);
    }

    public void testGetRequestSslCertificateSuccessfulInSandbox()
            throws BraintreeException, ErrorWithResponse {
        HttpRequest httpRequest = new HttpRequest("https://api.sandbox.braintreegateway.com",
                "myAuthFingerprint");

        int statusCode = httpRequest.get("/").getResponseCode();

        assertEquals(200, statusCode);
    }

    public void testGetRequestSslCertificateSuccessfulInProduction()
            throws BraintreeException, ErrorWithResponse {
        HttpRequest httpRequest = new HttpRequest("https://api.braintreegateway.com",
                "myAuthFingerprint");

        int statusCode = httpRequest.get("/").getResponseCode();

        assertEquals(200, statusCode);
    }

    public void testGetRequestBadCertificateCheck() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }
        try {
            HttpRequest httpRequest = new HttpRequest("https://" +
                    EnvironmentHelper.getLocalhostIp() + ":9443", "");
            httpRequest.setConnectTimeout(1000);
            httpRequest.get("/");
            fail();
        } catch (Exception e) {
            assertEquals("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.", e.getMessage());
        }
    }

    /* helpers */
    private void assertExceptionIsThrown(HttpURLConnection httpURLConnection,
            Class<? extends Exception> exceptionType, String exceptionMessage) throws IOException {
        when(httpURLConnection.getOutputStream()).thenReturn(mock(OutputStream.class));

        HttpRequest httpRequest = spy(new HttpRequest("", ""));
        doReturn(httpURLConnection).when(httpRequest).init(anyString());

        try {
            httpRequest.get("/");
            fail("No exception was thrown");
        } catch (Exception e) {
            assertEquals(exceptionType, e.getClass());
            if (exceptionMessage != null) {
                assertEquals(exceptionMessage, e.getMessage());
            }
        }

        try {
            httpRequest.post("/", "{}");
            fail("No exception was thrown");
        } catch (Exception e) {
            assertEquals(exceptionType, e.getClass());
            if (exceptionMessage != null) {
                assertEquals(exceptionMessage, e.getMessage());
            }
        }
    }
}
