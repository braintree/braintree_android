package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.InvalidArgumentException;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClientKeyTest {

    @Test(timeout = 1000)
    public void fromString_acceptsAClientKey() throws InvalidArgumentException {
        Authorization clientKey = Authorization.fromString(CLIENT_KEY);

        assertEquals("development_testing_integration_merchant_id", clientKey.toString());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void fromString_throwsInvalidArgumentExceptionForNonClientKeys()
            throws InvalidArgumentException {
        Authorization.fromString("{}");
    }

    @Test(timeout = 1000)
    public void fromString_parsesEnvironment() throws InvalidArgumentException {
        ClientKey clientKey = (ClientKey) Authorization.fromString(CLIENT_KEY);

        assertEquals("development", clientKey.getEnvironment());
    }

    @Test(timeout = 1000)
    public void fromString_parsesMerchantId() throws InvalidArgumentException {
        ClientKey clientKey = (ClientKey) Authorization.fromString(CLIENT_KEY);

        assertEquals("integration_merchant_id", clientKey.getMerchantId());
    }

    @Test(timeout = 1000)
    public void fromString_setsUrlForDevelopment() throws InvalidArgumentException {
        ClientKey clientKey = (ClientKey) Authorization.fromString(CLIENT_KEY);

        assertEquals(BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/", clientKey.getUrl());
        assertEquals(BuildConfig.DEVELOPMENT_URL + "merchants/integration_merchant_id/client_api/v1/configuration", clientKey.getConfigUrl());
    }

    @Test(timeout = 1000)
    public void fromString_setsUrlForSandbox() throws InvalidArgumentException {
        ClientKey clientKey = (ClientKey) Authorization.fromString("sandbox_fjajdkd_integration_merchant_id");

        assertEquals("https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/", clientKey.getUrl());
        assertEquals("https://api.sandbox.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration", clientKey.getConfigUrl());
    }

    @Test(timeout = 1000)
    public void fromString_setsUrlForProduction() throws InvalidArgumentException {
        ClientKey clientKey = (ClientKey) Authorization.fromString("production_fjajdkd_integration_merchant_id");

        assertEquals("https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/", clientKey.getUrl());
        assertEquals("https://api.braintreegateway.com/merchants/integration_merchant_id/client_api/v1/configuration", clientKey.getConfigUrl());
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void fromString_throwsInvalidArgumentExceptionForInvalidEnvironments() throws InvalidArgumentException {
        Authorization.fromString("test_fjajdkd_integration_merchant_id");
    }
}
