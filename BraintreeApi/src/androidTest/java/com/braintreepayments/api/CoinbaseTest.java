package com.braintreepayments.api;

import android.content.Intent;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.models.CoinbaseConfiguration;
import com.braintreepayments.api.models.Configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoinbaseTest extends AndroidTestCase {

    public void testIsAvailableReturnsTrueWhenCoinbaseIsEnabled() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isCoinbaseEnabled()).thenReturn(true);

        Coinbase coinbase = new Coinbase(getContext(), configuration);

        assertTrue(coinbase.isAvailable());
    }

    public void testIsAvailableReturnsFalseWhenCoinbaseIsDisabled() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isCoinbaseEnabled()).thenReturn(false);

        Coinbase coinbase = new Coinbase(getContext(), configuration);

        assertFalse(coinbase.isAvailable());
    }

    public void testGetLaunchIntentReturnsAnIntentToOpenUpTheCoinbaseWebsite()
            throws AppSwitchNotAvailableException {
        CoinbaseConfiguration coinbaseConfiguration = mock(CoinbaseConfiguration.class);
        when(coinbaseConfiguration.getClientId()).thenReturn("some-coinbase-client-id");
        when(coinbaseConfiguration.getMerchantAccount()).thenReturn("coinbase-merchant@example.com");
        when(coinbaseConfiguration.getRedirectUrl()).thenReturn(
                "com.example.merchant.payments.return://app/coinbase");
        when(coinbaseConfiguration.getScopes()).thenReturn("some coinbase scope");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getCoinbase()).thenReturn(coinbaseConfiguration);

        Intent coinbaseIntent = new Coinbase(getContext(), configuration).getLaunchIntent();

        assertEquals(Intent.ACTION_VIEW, coinbaseIntent.getAction());

        Uri browserSwitchUri = coinbaseIntent.getData();
        assertEquals("https", browserSwitchUri.getScheme());
        assertEquals("www.coinbase.com", browserSwitchUri.getHost());
        assertEquals("/oauth/authorize", browserSwitchUri.getPath());
        assertEquals("some-coinbase-client-id", browserSwitchUri.getQueryParameter("client_id"));
        assertEquals("coinbase-merchant@example.com", browserSwitchUri.getQueryParameter("meta[authorizations_merchant_account]"));
        assertEquals("com.example.merchant.payments.return://app/coinbase", browserSwitchUri.getQueryParameter("redirect_uri"));
        assertEquals("some coinbase scope", browserSwitchUri.getQueryParameter("scope"));
    }
}
