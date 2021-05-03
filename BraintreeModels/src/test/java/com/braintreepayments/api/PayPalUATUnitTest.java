package com.braintreepayments.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Base64;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalUATUnitTest {

    @Test
    public void fromString_setsAllProperties() {
        PayPalUAT payPalUAT = (PayPalUAT) Authorization.fromString(Fixtures.BASE64_PAYPAL_UAT);

        assertEquals("https://api.sandbox.braintreegateway.com:443/merchants/cfxs3ghzwfk2rhqm/client_api/v1/configuration", payPalUAT.getConfigUrl());
        assertEquals(Fixtures.BASE64_PAYPAL_UAT, payPalUAT.getBearer());
        assertEquals("https://api.sandbox.paypal.com", payPalUAT.getPayPalURL());
        assertEquals(PayPalUAT.Environment.SANDBOX, payPalUAT.getEnvironment());
    }

    // "iss" field properly indicates env

    @Test
    public void fromString_withStagingIssuer_setsProperEnv() {
        PayPalUAT payPalUAT = (PayPalUAT) Authorization.fromString(encodeUAT("{\"iss\":\"https://api.msmaster.qa.paypal.com\", \"external_id\":[\"Braintree:id\"]}"));

        assertEquals(PayPalUAT.Environment.STAGING, payPalUAT.getEnvironment());
    }

    @Test
    public void fromString_withSandboxIssuer_setsProperEnv() {
        PayPalUAT payPalUAT = (PayPalUAT) Authorization.fromString(encodeUAT("{\"iss\":\"https://api.paypal.com\", \"external_id\":[\"Braintree:id\"]}"));

        assertEquals(PayPalUAT.Environment.PRODUCTION, payPalUAT.getEnvironment());
    }

    // error scenarios

    @Test
    public void fromString_withMalformedUAT_returnsAnInvalidToken() {
        Authorization result = PayPalUAT.fromString("invalid.uat-without-signature");

        assertTrue(result instanceof InvalidAuthorization);
        String expectedErrorMessage = "Authorization provided is invalid: invalid.uat-without-signature";
        assertEquals(expectedErrorMessage, ((InvalidAuthorization) result).getErrorMessage());
    }

    @Test
    public void fromString_whenJSONSerializationFails_returnsAnInvalidToken() {
        Authorization result = PayPalUAT.fromString(encodeUAT("{\"some_invalid_json\": "));

        assertTrue(result instanceof InvalidAuthorization);
    }

    @Test
    public void fromString_whenNoBraintreeMerchantID_returnsAnInvalidToken() {
        Authorization result = PayPalUAT.fromString(encodeUAT("{\"iss\":\"paypal-url.com\", \"external_id\":[\"Faketree:my-merchant-id\"]}"));

        assertTrue(result instanceof InvalidAuthorization);
        String expectedErrorMessage = "PayPal UAT invalid: Missing Braintree merchant account ID.";
        assertEquals(expectedErrorMessage, ((InvalidAuthorization) result).getErrorMessage());
    }

    @Test
    public void fromString_whenNoIssuerPresent_returnsAnInvalidToken() {
        Authorization result = PayPalUAT.fromString(encodeUAT("{\"external_id\":[\"Braintree:my-merchant-id\"]}"));

        assertTrue(result instanceof InvalidAuthorization);
        String expectedErrorMessage = "PayPal UAT invalid: Does not contain issuer, or \"iss\" key.";
        assertEquals(expectedErrorMessage, ((InvalidAuthorization) result).getErrorMessage());
    }

    @Test
    public void fromString_whenPayPalURLUnknown_returnsAnInvalidToken() {
        Authorization result = PayPalUAT.fromString(encodeUAT("{\"iss\":\"fake-url.com\", \"external_id\":[\"Braintree:my-merchant-id\"]}"));

        assertTrue(result instanceof InvalidAuthorization);
        String expectedErrorMessage = "PayPal UAT invalid: PayPal issuer URL missing or unknown: fake-url.com";
        assertEquals(expectedErrorMessage, ((InvalidAuthorization) result).getErrorMessage());
    }

    // Test Helpers

    static String encodeUAT(String jsonString) {
        String encodedBody = Base64.getEncoder().withoutPadding().encodeToString(jsonString.getBytes());
        return "header." + encodedBody + ".footer";
    }

}
