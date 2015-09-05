package com.braintreepayments.api;

import android.content.Intent;
import android.os.Bundle;

import com.braintreepayments.api.models.AndroidPayConfiguration;
import com.braintreepayments.api.models.Configuration;
import com.google.android.gms.wallet.WalletConstants;

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

        AndroidPay androidPay = new AndroidPay(configuration);
        Bundle tokenizationParameters = androidPay.getTokenizationParameters().getParameters();

        assertEquals("braintree", tokenizationParameters.getString("gateway"));
        assertEquals(configuration.getMerchantId(), tokenizationParameters.getString("braintree:merchantId"));
        assertEquals(androidPayConfiguration.getGoogleAuthorizationFingerprint(), tokenizationParameters.getString("braintree:authorizationFingerprint"));
        assertEquals("v1", tokenizationParameters.getString("braintree:apiVersion"));
        assertEquals(BuildConfig.VERSION_NAME, tokenizationParameters.getString("braintree:sdkVersion"));
    }

    public void testIsMaskedWalletResponseReturnsTrueForMaskedWalletResponses() {
        Intent intent = new Intent().putExtra(WalletConstants.EXTRA_MASKED_WALLET, "");

        assertTrue(AndroidPay.isMaskedWalletResponse(intent));
    }

    public void testIsMaskedWalletResponseReturnsFalseForNonMaskedWalletResponses() {
        assertFalse(AndroidPay.isMaskedWalletResponse(new Intent()));
    }

    public void testIsFullWalletResponseReturnsTrueForFullWalletResponses() {
        Intent intent = new Intent().putExtra(WalletConstants.EXTRA_FULL_WALLET, "");

        assertTrue(AndroidPay.isFullWalletResponse(intent));
    }

    public void testIsFullWalletResponseReturnsFalseForNonFullWalletResponses() {
        assertFalse(AndroidPay.isFullWalletResponse(new Intent()));
    }
}
