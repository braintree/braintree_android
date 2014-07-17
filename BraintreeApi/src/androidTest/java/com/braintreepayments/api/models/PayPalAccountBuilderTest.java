package com.braintreepayments.api.models;

import com.braintreepayments.api.Utils;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

public class PayPalAccountBuilderTest extends TestCase {

    public void testBuildsAPayPalAccountCorrectly() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .email("test_email")
                .correlationId("correlation_id")
                .authorizationCode("test_auth_code");

        PayPalAccount paypalAccount = paypalAccountBuilder.build();
        JSONObject builtAccount = new JSONObject(Utils.getGson().toJson(paypalAccount));

        assertNull(builtAccount.opt("details"));
        assertEquals("test_auth_code", builtAccount.getString("consentCode"));
        assertEquals("correlation_id", builtAccount.getString("correlationId"));
    }

    public void testIncludesValidateOptionWhenSet() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .validate(true);

        JSONObject builtAccount = new JSONObject(Utils.getGson().toJson(paypalAccountBuilder.build()));

        assertEquals(true, builtAccount.getJSONObject("options").getBoolean("validate"));
    }

    public void testDoesNotIncludeEmptyObjectsWhenSerializing() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder();

        JSONObject builtAccount = new JSONObject(Utils.getGson().toJson(payPalAccountBuilder.build()));

        assertFalse(builtAccount.keys().hasNext());
    }
}
