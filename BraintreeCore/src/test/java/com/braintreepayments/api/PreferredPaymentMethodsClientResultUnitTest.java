package com.braintreepayments.api;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreferredPaymentMethodsClientResultUnitTest {

    @Test
    public void fromJson_whenApiDetectsPayPalPreferred_setsPayPalPreferredToTrue() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("{\n" +
                "  \"data\": {\n" +
                "    \"preferredPaymentMethods\": {\n" +
                "      \"paypalPreferred\": true\n" +
                "    }\n" +
                "  }\n" +
                "}", false);

        assertTrue(result.isPayPalPreferred());
    }

    @Test
    public void fromJson_whenApiDetectsPayPalNotPreferred_setsPayPalPreferredToFalse() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("{\n" +
                "  \"data\": {\n" +
                "    \"preferredPaymentMethods\": {\n" +
                "      \"paypalPreferred\": false\n" +
                "    }\n" +
                "  }\n" +
                "}", false);

        assertFalse(result.isPayPalPreferred());
    }

    @Test
    public void fromJson_whenVenmoAppIsInstalled_setsVenmoPreferredToTrue() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("json", true);
        assertTrue(result.isVenmoPreferred());
    }

    @Test
    public void fromJson_whenVenmoAppIsNotInstalled_setsVenmoPreferredToFalse() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("json", false);
        assertFalse(result.isVenmoPreferred());
    }

    @Test
    public void fromJson_whenJsonIsInvalid_setsIsPayPalPreferredToFalse() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("invalid-response", false);

        assertFalse(result.isPayPalPreferred());
        assertFalse(result.isVenmoPreferred());
    }
}