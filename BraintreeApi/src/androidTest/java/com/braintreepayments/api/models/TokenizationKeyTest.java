package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.InvalidArgumentException;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TokenizationKeyTest {

    @Test(timeout = 1000)
    public void fromString_acceptsATokenizationKey() throws InvalidArgumentException {
        Authorization tokenizationKey = Authorization.fromString(TOKENIZATION_KEY);

        assertEquals("development_testing_integration_merchant_id", tokenizationKey.toString());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void fromString_throwsInvalidArgumentExceptionForNonTokenizationKeys()
            throws InvalidArgumentException {
        Authorization.fromString("{}");
    }

    @Test(timeout = 1000)
    public void fromString_parsesEnvironment() throws InvalidArgumentException {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(
                TOKENIZATION_KEY);

        assertEquals("development", tokenizationKey.getEnvironment());
    }

    @Test(timeout = 1000)
    public void fromString_parsesMerchantId() throws InvalidArgumentException {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(
                TOKENIZATION_KEY);

        assertEquals("integration_merchant_id", tokenizationKey.getMerchantId());
    }

    @Test(timeout = 1000)
    public void fromString_setsUrlForDevelopment() throws InvalidArgumentException {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString(
                TOKENIZATION_KEY);

        assertEquals(BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/", tokenizationKey
                .getUrl());
        assertEquals(BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/v1/configuration", tokenizationKey
                .getConfigUrl());
    }

    @Test(timeout = 1000)
    public void fromString_setsUrlForSandbox() throws InvalidArgumentException {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString("sandbox_fjajdkd_integration_merchant_id");

        assertEquals("https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/", tokenizationKey
                .getUrl());
        assertEquals("https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration", tokenizationKey
                .getConfigUrl());
    }

    @Test(timeout = 1000)
    public void fromString_setsUrlForProduction() throws InvalidArgumentException {
        TokenizationKey tokenizationKey = (TokenizationKey) Authorization.fromString("production_fjajdkd_integration_merchant_id");

        assertEquals("https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/", tokenizationKey
                .getUrl());
        assertEquals("https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration", tokenizationKey
                .getConfigUrl());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void fromString_throwsInvalidArgumentExceptionForInvalidEnvironments() throws InvalidArgumentException {
        Authorization.fromString("test_fjajdkd_integration_merchant_id");
    }
}
