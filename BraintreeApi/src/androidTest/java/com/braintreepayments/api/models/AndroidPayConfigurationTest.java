package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import static com.braintreepayments.api.TestUtils.getConfigurationFromFixture;

public class AndroidPayConfigurationTest extends AndroidTestCase {

    public void testParsesAndroidPayConfigurationFromToken() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_android_pay.json");

        AndroidPayConfiguration androidPayConfiguration = configuration.getAndroidPay();

        assertTrue(androidPayConfiguration.isEnabled());
        assertEquals("google-auth-fingerprint", androidPayConfiguration.getGoogleAuthorizationFingerprint());
        assertEquals("Android Pay Merchant", androidPayConfiguration.getDisplayName());
        assertEquals("sandbox", androidPayConfiguration.getEnvironment());
    }
}
