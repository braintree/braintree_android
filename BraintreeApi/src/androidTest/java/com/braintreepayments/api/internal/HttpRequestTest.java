package com.braintreepayments.api.internal;

import com.braintree.api.test.BuildConfig;

import junit.framework.TestCase;

public class HttpRequestTest extends TestCase {

    public void testIncludesAppAndDeviceInfoInUserAgent() {
        HttpRequest httpRequest = new HttpRequest(null, null, null);

        String expected = "braintree/android/" + BuildConfig.VERSION_NAME;

        assertEquals(expected, httpRequest.getUserAgent());
    }
}
