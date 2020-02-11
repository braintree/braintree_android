package com.braintreepayments.api.models;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreferredPaymentMethodsResultTest {

    @Test
    public void fromJson_whenPayPalIsPreferred_setsIsPayPalPreferredToTrue() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("{\n" +
                "  \"data\": {\n" +
                "    \"clientConfiguration\": {\n" +
                "      \"paypal\": {\n" +
                "        \"preferredPaymentMethod\": true\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertTrue(result.isPayPalPreferred());
    }

    @Test
    public void fromJson_whenPayPalIsNotPreferred_setsIsPayPalPreferredToFalse() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("{\n" +
                "  \"data\": {\n" +
                "    \"clientConfiguration\": {\n" +
                "      \"paypal\": {\n" +
                "        \"preferredPaymentMethod\": false\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");

        assertFalse(result.isPayPalPreferred());
    }

    @Test
    public void fromJson_whenJsonIsInvalid_setsIsPayPalPreferredToFalse() {
        PreferredPaymentMethodsResult result = PreferredPaymentMethodsResult.fromJSON("invalid-response");

        assertFalse(result.isPayPalPreferred());
    }
}