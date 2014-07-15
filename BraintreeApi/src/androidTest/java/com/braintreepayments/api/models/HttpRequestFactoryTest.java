package com.braintreepayments.api.models;

import android.test.InstrumentationTestCase;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.EnvironmentHelper;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest.HttpMethod;
import com.braintreepayments.api.internal.HttpRequestFactory;

public class HttpRequestFactoryTest extends InstrumentationTestCase {

    public void testGetRequestSslCertificateSuccessfulInSandbox() throws UnexpectedException {
        int statusCode = getHttpRequestFactory()
                .getRequest(HttpMethod.GET, "https://api.sandbox.braintreegateway.com")
                .execute()
                .statusCode();

        assertEquals(200, statusCode);
    }

    public void testGetRequestSslCertificateSuccessfulInProduction() throws UnexpectedException {
        int statusCode = getHttpRequestFactory()
                .getRequest(HttpMethod.GET, "https://api.braintreegateway.com")
                .execute()
                .statusCode();

        assertEquals(200, statusCode);
    }

    public void testHostnameVerificationFailsForIncorrectHostName() {
        try {
            getHttpRequestFactory().getRequest(HttpMethod.GET, "https://204.109.13.121:443")
                    .execute();
            fail();
        } catch (Exception e) {
            assertEquals("Hostname '204.109.13.121' was not verified", e.getMessage());
        }
    }

    public void testGetRequestBadCertificateCheck() {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }
        try {
            getHttpRequestFactory().getRequest(HttpMethod.GET, "https://" +
                    EnvironmentHelper.getLocalhostIp() + ":9443")
                    .execute();
            fail();
        } catch (Exception e) {
            // @formatter:off
            assertEquals("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.", e.getMessage());
            // @formatter:on
        }
    }

    private HttpRequestFactory getHttpRequestFactory() {
        return new HttpRequestFactory();
    }
}
