package com.braintreepayments.api;

import android.os.Bundle;

import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.Configuration;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AndroidPayTest extends TestCase {

    public void testGetTokenizationParametersReturnsCorrectParameters() {
        ClientToken clientToken = mock(ClientToken.class);
        when(clientToken.getAuthorizationFingerprint()).thenReturn("test-auth-fingerprint");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getMerchantId()).thenReturn("android-pay-merchant-id");

        AndroidPay androidPay = new AndroidPay(clientToken, configuration, null);
        Bundle tokenizationParameters = androidPay.getTokenizationParameters().getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(clientToken.getAuthorizationFingerprint(), tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
    }
}
