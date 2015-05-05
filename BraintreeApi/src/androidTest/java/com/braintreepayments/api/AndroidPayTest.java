package com.braintreepayments.api;

import android.os.Bundle;

import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Configuration;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AndroidPayTest extends TestCase {

    public void testGetTokenizationParametersReturnsCorrectParameters() {
        AndroidPayConfiguration androidPayConfiguration = mock(AndroidPayConfiguration.class);
        when(androidPayConfiguration.getGoogleAuthorizationFingerprint()).thenReturn("google-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");
        when(configuration.getAndroidPay()).thenReturn(androidPayConfiguration);

        AndroidPay androidPay = new AndroidPay(configuration, null);
        Bundle tokenizationParameters = androidPay.getTokenizationParameters().getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(androidPayConfiguration.getGoogleAuthorizationFingerprint(), tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
    }
}
