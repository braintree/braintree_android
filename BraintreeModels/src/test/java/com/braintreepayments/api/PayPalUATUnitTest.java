package com.braintreepayments.api;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Base64;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalUATUnitTest {
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

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
    public void fromString_withMalformedUAT_shouldReturnAnInvalidToken() {
        Authorization result = PayPalUAT.fromString("invalid.uat-without-signature");

        assertTrue(result instanceof InvalidToken);
        String expectedErrorMessage = "Authorization provided is invalid: invalid.uat-without-signature";
        assertEquals(expectedErrorMessage, ((InvalidToken) result).getErrorMessage());
    }

    @Test
    public void fromString_whenJSONSerializationFails_throwsException() {
        exceptionRule.expect(InvalidArgumentException.class);

        PayPalUAT.fromString(encodeUAT("{\"some_invalid_json\": "));
    }

    @Test
    public void fromString_whenNoBraintreeMerchantID_throwsException() {
        exceptionRule.expect(InvalidArgumentException.class);
        exceptionRule.expectMessage("PayPal UAT invalid: Missing Braintree merchant account ID.");

        PayPalUAT.fromString(encodeUAT("{\"iss\":\"paypal-url.com\", \"external_id\":[\"Faketree:my-merchant-id\"]}"));
    }

    @Test
    public void fromString_whenNoIssuerPresent_throwsException() {
        exceptionRule.expect(InvalidArgumentException.class);
        exceptionRule.expectMessage("PayPal UAT invalid: Does not contain issuer, or \"iss\" key.");

        PayPalUAT.fromString(encodeUAT("{\"external_id\":[\"Braintree:my-merchant-id\"]}"));
    }

    @Test
    public void fromString_whenPayPalURLUnknown_throwsException() {
        exceptionRule.expect(InvalidArgumentException.class);
        exceptionRule.expectMessage("PayPal issuer URL missing or unknown: fake-url.com");

        PayPalUAT.fromString(encodeUAT("{\"iss\":\"fake-url.com\", \"external_id\":[\"Braintree:my-merchant-id\"]}"));
    }

    // Test Helpers

    static String encodeUAT(String jsonString) {
        String encodedBody = Base64.getEncoder().withoutPadding().encodeToString(jsonString.getBytes());
        return "header." + encodedBody + ".footer";
    }

}
