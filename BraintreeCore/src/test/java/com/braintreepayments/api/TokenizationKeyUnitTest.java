package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class TokenizationKeyUnitTest {

    private static final String TOKENIZATION_KEY = "development_testing_integration_merchant_id";

    @Test
    public void fromString_acceptsATokenizationKey() {
        Authorization tokenizationKey = Authorization.fromString(TOKENIZATION_KEY);

        assertEquals(TOKENIZATION_KEY, tokenizationKey.getBearer());
    }

    @Test
    public void fromString_returnsInvalidTokenForNonTokenizationKeys() {
        Authorization result = Authorization.fromString("{}");

        assertTrue(result instanceof InvalidAuthorization);
    }

    @Test
    public void fromString_parsesEnvironment() {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(TOKENIZATION_KEY);

        assertEquals("development", tokenizationKey.getEnvironment());
    }

    @Test
    public void fromString_parsesMerchantId() {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(TOKENIZATION_KEY);

        assertEquals("integration_merchant_id", tokenizationKey.getMerchantId());
    }

    @Test
    public void fromString_setsUrlForDevelopment() {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(TOKENIZATION_KEY);

        assertEquals(BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/",
                tokenizationKey.getUrl());
        assertEquals(BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/v1/configuration",
                tokenizationKey.getConfigUrl());
    }

    @Test
    public void fromString_setsUrlForSandbox() {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(
                "sandbox_fjajdkd_integration_merchant_id");

        assertEquals("https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/",
                tokenizationKey.getUrl());
        assertEquals("https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration",
                tokenizationKey.getConfigUrl());
    }

    @Test
    public void fromString_setsUrlForProduction() {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(
                "production_fjajdkd_integration_merchant_id");

        assertEquals("https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/",
                tokenizationKey.getUrl());
        assertEquals("https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration",
                tokenizationKey.getConfigUrl());
    }

    @Test
    public void fromString_returnsInvalidTokenForInvalidEnvironments() {
        Authorization result = Authorization.fromString("test_fjajdkd_integration_merchant_id");

        assertTrue(result instanceof InvalidAuthorization);
    }

    @Test
    public void getBearer_returnsTokenizationKey() {
        assertEquals(TOKENIZATION_KEY, Authorization.fromString(TOKENIZATION_KEY).getBearer());
    }
}
