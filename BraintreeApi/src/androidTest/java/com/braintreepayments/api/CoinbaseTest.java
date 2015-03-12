package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.braintreepayments.api.Coinbase.Response;
import com.braintreepayments.api.exceptions.AppSwitchNotAvailableException;
import com.braintreepayments.api.models.CoinbaseConfiguration;
import com.braintreepayments.api.models.Configuration;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoinbaseTest extends TestCase {

    public void testIsAvailableReturnsTrueWhenCoinbaseIsEnabled() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isCoinbaseEnabled()).thenReturn(true);

        Coinbase coinbase = getCoinbaseWithSpecifiedConfiguration(configuration);

        assertTrue(coinbase.isAvailable());
    }

    public void testIsAvailableReturnsFalseWhenCoinbaseIsDisabled() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.isCoinbaseEnabled()).thenReturn(false);

        Coinbase coinbase = getCoinbaseWithSpecifiedConfiguration(configuration);

        assertFalse(coinbase.isAvailable());
    }

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
        assertEquals("some-coinbase-client-id", browserSwitchUri.getQueryParameter("client_id"));
        assertEquals("coinbase-merchant@example.com", browserSwitchUri.getQueryParameter("meta[authorizations_merchant_account]"));
        assertNotNull(browserSwitchUri.getQueryParameter("redirect_uri"));
        assertEquals("some coinbase scope", browserSwitchUri.getQueryParameter("scope"));
    }

    public void testCanHandleResponseAcceptsAValidResponse() throws UnsupportedEncodingException {
        Intent coinbaseIntent = getCoinbaseWithValidConfiguration().getLaunchIntent();

        Uri browserSwitchUri = Uri.parse(coinbaseIntent.getStringExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL));
        Uri redirectUri = Uri.parse(browserSwitchUri.getQueryParameter("redirect_uri"))
                .buildUpon()
                .appendQueryParameter("code", "1234")
                .build();

        assertTrue("Coinbase should handle a round trip redirect success",
                getCoinbaseWithValidConfiguration().canHandleResponse(redirectUri));
    }

    public void testCanHandleResponseRejectsAnInvalidResponse() {
        Uri uri = Uri.parse("my.app.social.stuff://not-for-braintree");

        assertFalse("Coinbase should reject a random url", getCoinbaseWithValidConfiguration().canHandleResponse(uri));
    }

    public void testHandleResponseReturnsACoinbaseResponse() throws UnsupportedEncodingException {
        Coinbase coinbase = getCoinbaseWithValidConfiguration();

        Uri browserSwitchUri = Uri.parse(coinbase.getLaunchIntent()
                .getStringExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_URL));
        Uri responseUri = Uri.parse(browserSwitchUri.getQueryParameter("redirect_uri"))
                .buildUpon()
                .appendQueryParameter("code", "1234")
                .build();

        Response response = coinbase.handleResponse(responseUri);

        assertNotNull("handleResponse should return a CoinbaseResponse", response);
        assertTrue("handleResponse returns a success response when code is present",
                response.isSuccess());
    }

    public void testCoinbaseReturnParsesCodeOnSuccess() {
        Coinbase.Response coinbaseResponse = getCoinbaseWithValidConfiguration().handleResponse(
                Uri.parse("com.example.merchant.braintree://coinbase?code=some-code-from-coinbase"));

        assertTrue("Response should be interpreted as successful", coinbaseResponse.isSuccess());
        assertEquals("Response should parse code from URI", "some-code-from-coinbase",
                coinbaseResponse.getCode());
    }

    public void testCoinbaseReturnParsesErrorOnFailure() {
        Coinbase.Response coinbaseResponse = getCoinbaseWithValidConfiguration().handleResponse(
                Uri.parse(
                        "com.example.merchant.braintree://coinbase?error_description=Something%20went%20wrong!&error_code=FAIL"));

        assertFalse("Response should be interpreted as failure", coinbaseResponse.isSuccess());
        assertEquals("Response should parse error message from URI", "Something went wrong!",
                coinbaseResponse.getErrorMessage());
    }

    public void testCoinbaseIsSuccessfulReturnsFalseUnlessCodeIsPresent() {
        Coinbase.Response coinbaseResponse = getCoinbaseWithValidConfiguration().handleResponse(
                Uri.parse("com.example.merchant.braintree://coinbase?unexpected=most_definitely"));

        assertFalse("Response should be interpreted as failure by default",
                coinbaseResponse.isSuccess());
        assertNull("Response code is not present", coinbaseResponse.getCode());
        assertNull("Response error message is not present", coinbaseResponse.getErrorMessage());
    }

    public void testReturnsNullForAURIWithAnUnexpectedScheme() {
        Coinbase.Response coinbaseResponse = getCoinbaseWithValidConfiguration().handleResponse(Uri.parse(
                "com.example.merchant.cool-social-media-login://coinbase?code=some-code-from-coinbase"));

        assertNull("Coinbase.Response should ignore an invalid scheme", coinbaseResponse);
    }

    public void testReturnsNullForAURIWithAnUnexpectedHost() {
        Coinbase.Response coinbaseResponse = getCoinbaseWithValidConfiguration().handleResponse(
                Uri.parse("com.example.merchant.braintree://dogecoin?code=some-code-from-coinbase"));

        assertNull("CoinbaseResponse should ignore an invalid scheme", coinbaseResponse);
    }

    public void testReturnsNullForNullURI() {
        Coinbase.Response coinbaseResponse = getCoinbaseWithValidConfiguration().handleResponse(null);

        assertNull("CoinbaseResponse should ignore a null URI", coinbaseResponse);
    }

    /* helpers */
    private Coinbase getCoinbaseWithValidConfiguration() {
        CoinbaseConfiguration coinbaseConfiguration = mock(CoinbaseConfiguration.class);
        when(coinbaseConfiguration.getClientId()).thenReturn("some-coinbase-client-id");
        when(coinbaseConfiguration.getMerchantAccount()).thenReturn("coinbase-merchant@example.com");
        when(coinbaseConfiguration.getScopes()).thenReturn("some coinbase scope");
        Configuration configuration = mock(Configuration.class);
        when(configuration.getCoinbase()).thenReturn(coinbaseConfiguration);

        return getCoinbaseWithSpecifiedConfiguration(configuration);
    }

    private Coinbase getCoinbaseWithSpecifiedConfiguration(Configuration configuration) {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.example.merchant");
        return new Coinbase(context, configuration);
    }
}
