package com.braintreepayments.api.models;

import com.braintreepayments.api.models.PaymentMethod.Builder;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

public class PayPalAccountBuilderTest extends TestCase {

    private static final String PAYPAL_KEY = "paypalAccount";

    public void testBuildsAPayPalAccountCorrectly() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .email("test_email")
                .correlationId("correlation_id")
                .authorizationCode("test_auth_code")
                .source("paypal-sdk");

        JSONObject json = new JSONObject(paypalAccountBuilder.toJsonString());
        JSONObject jsonAccount = json.getJSONObject(PAYPAL_KEY);
        JSONObject jsonMetadata = json.getJSONObject(Builder.METADATA_KEY);

        assertNull(jsonAccount.opt("details"));
        assertEquals("test_auth_code", jsonAccount.getString("consentCode"));
        assertEquals("correlation_id", jsonAccount.getString("correlationId"));
        assertEquals("custom", jsonMetadata.getString("integration"));
        assertEquals("paypal-sdk", jsonMetadata.getString("source"));
    }

    public void testUsesCorrectInfoForMetadata() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder()
                .source("paypal-app");

        JSONObject metadata = new JSONObject(payPalAccountBuilder.toJsonString()).getJSONObject(
                Builder.METADATA_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("paypal-app", metadata.getString("source"));
    }

    public void testSetsIntegrationMethod() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder().integration("test-integration");

        JSONObject metadata = new JSONObject(payPalAccountBuilder.toJsonString()).getJSONObject(Builder.METADATA_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    public void testIncludesValidateOptionWhenSet() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .validate(true);

        JSONObject builtAccount = new JSONObject(paypalAccountBuilder.toJsonString()).getJSONObject(PAYPAL_KEY);

        assertEquals(true, builtAccount.getJSONObject("options").getBoolean("validate"));
    }

    public void testDoesNotIncludeEmptyObjectsWhenSerializing() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder();

        JSONObject builtAccount = new JSONObject(payPalAccountBuilder.toJsonString()).getJSONObject(PAYPAL_KEY);

        assertFalse(builtAccount.keys().hasNext());
    }
}
