package com.braintreepayments.api.internal;

import com.braintreepayments.api.BuildConfig;

import junit.framework.TestCase;

import org.apache.http.message.BasicNameValuePair;

import java.util.Locale;

public class HttpRequestTest extends TestCase {

    private HttpRequest mHttpRequest;

    @Override
    public void setUp() {
        mHttpRequest = new HttpRequest(null, null, null);
    }

    public void testSendsUserAgent() {
        assertTrue(hasHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME));
    }

    public void testSendsAcceptLanguageHeader() {
        assertTrue(hasHeader("Accept-Language", Locale.getDefault().getLanguage()));
    }

    private boolean hasHeader(String headerKey, String headerValue) {
        return mHttpRequest.getHeaders().contains(new BasicNameValuePair(headerKey, headerValue));
    }
}
