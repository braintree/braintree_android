package com.braintreepayments.api;

import android.test.AndroidTestCase;

import java.io.IOException;

public class ClientTokenTest extends AndroidTestCase {

    public void testCanInstantiateFromJsonString() {
        String clientToken = FixturesHelper.stringFromFixture(getContext(),
                "client_tokens/client_token.json");

        ClientToken parsedClientToken = ClientToken.getClientToken(clientToken);

        assertEquals("client_api_url", parsedClientToken.getClientApiUrl());
    }

    public void testCanInstantiateFromBase64String() {
        String clientToken = FixturesHelper.stringFromFixture(getContext(),
                "client_tokens/client_token_v2.txt");

        ClientToken parsedClientToken = ClientToken.getClientToken(clientToken);

        assertEquals("http://localhost:3000/merchants/t8xxjgyyn9vwpbk3/client_api", parsedClientToken.getClientApiUrl());
    }

    public void testParsesSingleChallengeFromToken() throws IOException {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json");

        assertTrue(token.isCvvChallengePresent());
        assertFalse(token.isPostalCodeChallengePresent());
    }

    public void testParsesAllChallengesFromToken() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token_with_challenges.json");

        assertTrue(token.isCvvChallengePresent());
        assertTrue(token.isPostalCodeChallengePresent());
    }

    public void testParsesMerchantIdFromToken() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json");

        assertEquals("integration_merchant_id", token.getMerchantId());
    }

    public void testParsesAuthorizationFingerprintFromToken() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json");

        assertEquals("authorization_fingerprint", token.getAuthorizationFingerprint());
    }

    public void testParsesPayPalConfigurationFromToken() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/offline_paypal_client_token.json");

        assertTrue(token.isPayPalEnabled());

        assertEquals("paypal_merchant", token.getPayPal().getDisplayName());
        assertEquals("paypal_client_id", token.getPayPal().getClientId());
        assertEquals("http://www.example.com/privacy", token.getPayPal().getPrivacyUrl());
        assertEquals("http://www.example.com/user_agreement", token.getPayPal().getUserAgreementUrl());
        assertEquals("http://localhost:9000/v1/", token.getPayPal().getDirectBaseUrl());
        assertEquals("offline", token.getPayPal().getEnvironment());
        assertTrue(token.getPayPal().getAllowHttp());
    }


    public void testReportsPayPalNotEnabledWhenFlagged() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json");

        assertFalse(token.isPayPalEnabled());
    }

    public void testReportsPayPalNotEnabledWhenConfigAbsent() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token_paypal_enabled_and_missing.json");

        assertFalse(token.isPayPalEnabled());
    }

    public void testReturnsOffIfVenmoIsNull() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token_venmo_missing.json");

        assertEquals("off", token.getVenmoState());
    }

    public void testReturnsVenmoStatus() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token_venmo_offline.json");

        assertEquals("offline", token.getVenmoState());
    }

    public void testParsesAnalyticConfigurationFromToken() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token_analytics.json");

        assertNotNull(token.getAnalytics());
        assertTrue(token.isAnalyticsEnabled());
        assertEquals("analytics_url", token.getAnalytics().getUrl());
    }

    public void testExposesPayPalTouchKillSwitch() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token_paypal_touch_killswitch.json");

        assertTrue(token.getPayPal().getTouchDisabled());
    }

    public void testReportsAnalyticsDisabledWhenNoAnalyticsPresent() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token.json");

        assertNull(token.getAnalytics());
        assertFalse(token.isAnalyticsEnabled());
    }

    public void testReportsAnalyticsDisabledWhenUrlIsEmpty() {
        ClientToken token = TestUtils.clientTokenFromFixture(getContext(), "client_tokens/client_token_analytics_empty_url.json");

        assertNotNull(token.getAnalytics());
        assertFalse(token.isAnalyticsEnabled());
    }
}
