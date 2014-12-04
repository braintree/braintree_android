package com.braintreepayments.api.internal;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.EnvironmentHelper;
import com.braintreepayments.api.exceptions.UnexpectedException;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Locale;

public class HttpRequestTest extends TestCase {

    private HttpRequest mHttpRequest;

    @Override
    public void setUp() {
        HttpRequest.DEBUG = true;
        mHttpRequest = new HttpRequest("myAuthorizationToken");
    }

    public void testSendsUserAgent() throws IOException {
        HttpURLConnection connection = mHttpRequest.init("http://example.com");
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME,
                connection.getRequestProperty("User-Agent"));
    }

    public void testSendsAcceptLanguageHeader() throws IOException {
        HttpURLConnection connection = mHttpRequest.init("http://example.com");
        assertEquals(Locale.getDefault().getLanguage(),
                connection.getRequestProperty("Accept-Language"));
    }

    public void testSendsContentType() throws IOException {
        HttpURLConnection connection = mHttpRequest.init("http://example.com");
        assertEquals("application/json", connection.getRequestProperty("Content-Type"));
    }

    public void testSetsAuthorizationTokenOnGet() throws UnexpectedException {
        assertEquals(EnvironmentHelper.getGatewayPath() + "/v1/payment_methods?authorizationFingerprint=myAuthorizationToken",
                mHttpRequest.get(EnvironmentHelper.getGatewayPath() + "/v1/payment_methods").getUrl());
    }

    public void testSetsAuthorizationTokenOnPost() throws UnexpectedException {
        assertTrue(mHttpRequest.post(EnvironmentHelper.getGatewayPath(), "{}").getData()
                .contains("myAuthorizationToken"));
    }

    public void testGetRequestSslCertificateSuccessfulInSandbox() throws UnexpectedException {
        int statusCode = mHttpRequest
                .get("https://api.sandbox.braintreegateway.com")
                .getResponseCode();

        assertEquals(200, statusCode);
    }

    public void testGetRequestSslCertificateSuccessfulInProduction() throws UnexpectedException {
        int statusCode = mHttpRequest
                .get("https://api.braintreegateway.com")
                .getResponseCode();

        assertEquals(200, statusCode);
    }

    public void testGetRequestBadCertificateCheck() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }
        try {
            mHttpRequest.setConnectTimeout(1000);
            mHttpRequest.get("https://" + EnvironmentHelper.getLocalhostIp() + ":9443/");
            fail();
        } catch (Exception e) {
            // @formatter:off
            assertEquals("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.", e.getMessage());
            // @formatter:on
        }
    }
}
