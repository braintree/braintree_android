package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.exceptions.CoinbaseException;
import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.models.CoinbaseConfiguration;
import com.braintreepayments.api.models.Configuration;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoinbaseTest {

    @Test
    public void testIsAvailableReturnsTrueWhenCoinbaseIsEnabled() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isCoinbaseEnabled()).thenReturn(true);

        Coinbase coinbase = getCoinbaseWithSpecifiedConfiguration(configuration);

        assertTrue(coinbase.isAvailable());
    }

    @Test
    public void testIsAvailableReturnsFalseWhenCoinbaseIsDisabled() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isCoinbaseEnabled()).thenReturn(false);

        Coinbase coinbase = getCoinbaseWithSpecifiedConfiguration(configuration);

        assertFalse(coinbase.isAvailable());
    }

    @Test
    public void testGetLaunchIntentReturnsAnIntentToOpenUpTheCoinbaseWebsite()
            throws AppSwitchNotAvailableException, UnsupportedEncodingException {
        Intent coinbaseIntent = getCoinbaseWithValidConfiguration().getLaunchIntent();

        assertEquals(BraintreeBrowserSwitchActivity.class.getCanonicalName(),
                coinbaseIntent.getComponent().getClassName());

        Uri browserSwitchUri = Uri.parse(
                coinbaseIntent.getStringExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL));
        assertEquals("https", browserSwitchUri.getScheme());
        assertEquals("www.coinbase.com", browserSwitchUri.getHost());
        assertEquals("/oauth/authorize", browserSwitchUri.getPath());
        assertEquals("some-coinbase-client-id",
                browserSwitchUri.getQueryParameter("client_id"));
        assertEquals("coinbase-merchant@example.com",
                browserSwitchUri.getQueryParameter("meta[authorizations_merchant_account]"));
        assertNotNull(browserSwitchUri.getQueryParameter("redirect_uri"));
        assertEquals("some coinbase scope", browserSwitchUri.getQueryParameter("scope"));
    }

    @Test
    public void testCanHandleResponseAcceptsAValidResponse() throws UnsupportedEncodingException {
        Intent coinbaseIntent = getCoinbaseWithValidConfiguration().getLaunchIntent();

        Uri browserSwitchUri = Uri.parse(coinbaseIntent.getStringExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL));
        Uri redirectUri = Uri.parse(browserSwitchUri.getQueryParameter("redirect_uri"))
                .buildUpon()
                .appendQueryParameter("code", "1234")
                .build();

        assertTrue("Coinbase should handle a round trip redirect success",
                Coinbase.canParseResponse(getContext(), redirectUri));
    }

    @Test
    public void testCanHandleResponseRejectsAnInvalidResponse() {
        Uri uri = Uri.parse("my.app.social.stuff://not-for-braintree");

        assertFalse("Coinbase should reject a random url",
                Coinbase.canParseResponse(getContext(), uri));
    }

    @Test
    public void testParseResponseReturnsACodeOnSuccess()
            throws UnsupportedEncodingException, CoinbaseException, ConfigurationException {
        Coinbase coinbase = getCoinbaseWithValidConfiguration();

        Uri browserSwitchUri = Uri.parse(coinbase.getLaunchIntent()
                .getStringExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL));
        Uri responseUri = Uri.parse(browserSwitchUri.getQueryParameter("redirect_uri"))
                .buildUpon()
                .appendQueryParameter("code", "some-code-from-coinbase")
                .build();

        assertEquals("Response should parse code from URI", "some-code-from-coinbase",
                coinbase.parseResponse(responseUri));
    }

    @Test
    public void testParseResponseThrowsWhenCoinbaseReturnsAnErrorMessage() {
        try {
            getCoinbaseWithValidConfiguration().parseResponse(
                    Uri.parse(
                            "com.example.merchant.braintree://coinbase?error_description=Something%20went%20wrong!&error_code=FAIL"));
            fail("No exception was thrown");
        } catch (CoinbaseException e) {
            assertEquals("Response should parse error message from URI",
                    "Something went wrong!",
                    e.getMessage());
        } catch (ConfigurationException e) {
            fail("Should not throw a ConfigurationException");
        }
    }

    @Test(expected = CoinbaseException.class)
    public void testParseResponseThrowsWhenCodeIsNotPresent()
            throws CoinbaseException, ConfigurationException {
            getCoinbaseWithValidConfiguration().parseResponse(
                    Uri.parse("com.example.merchant.braintree://coinbase?unexpected=most_definitely"));
    }

    @Test(expected = ConfigurationException.class)
    public void testParseResponseThrowsForAURIWithAnUnexpectedScheme()
            throws CoinbaseException, ConfigurationException {
        getCoinbaseWithValidConfiguration().parseResponse(
                Uri.parse(
                        "com.example.merchant.cool-social-media-login://coinbase?code=some-code-from-coinbase"));
    }

    @Test(expected = ConfigurationException.class)
    public void testParseResponseThrowsForAURIWithAnUnexpectedHost()
            throws CoinbaseException, ConfigurationException {
         getCoinbaseWithValidConfiguration().parseResponse(
                Uri.parse("com.example.merchant.braintree://dogecoin?code=some-code-from-coinbase"));
    }

    @Test(expected = ConfigurationException.class)
    public void testParseResponseThrowsForNullURI()
            throws CoinbaseException, ConfigurationException {
        getCoinbaseWithValidConfiguration().parseResponse(null);
    }

    /* helpers */
    private Coinbase getCoinbaseWithValidConfiguration() {
        try {
            CoinbaseConfiguration config = new CoinbaseConfiguration();
            Field clientId = CoinbaseConfiguration.class.getDeclaredField("clientId");
            Field merchantAccount = CoinbaseConfiguration.class.getDeclaredField("merchantAccount");
            Field scopes = CoinbaseConfiguration.class.getDeclaredField("scopes");
            Field environment = CoinbaseConfiguration.class.getDeclaredField("environment");

            clientId.setAccessible(true);
            merchantAccount.setAccessible(true);
            scopes.setAccessible(true);
            environment.setAccessible(true);

            clientId.set(config, "some-coinbase-client-id");
            merchantAccount.set(config, "coinbase-merchant@example.com");
            scopes.set(config, "some coinbase scope");
            environment.set(config, "sandbox_shared");

            Configuration configuration = mock(Configuration.class);
            when(configuration.getCoinbase()).thenReturn(config);

            return getCoinbaseWithSpecifiedConfiguration(configuration);
        } catch (IllegalAccessException iae) {
            return null;
        } catch (NoSuchFieldException nsf) {
            return null;
        }
    }

    private Coinbase getCoinbaseWithSpecifiedConfiguration(Configuration configuration) {
        return new Coinbase(getContext(), configuration);
    }

    private Context getContext() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.example.merchant");
        return context;
    }
}
