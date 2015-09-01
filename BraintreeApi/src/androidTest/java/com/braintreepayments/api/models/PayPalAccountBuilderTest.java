package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class PayPalAccountBuilderTest {

    private static final String PAYPAL_KEY = "paypalAccount";

    @Test(timeout = 1000)
    @SmallTest
    public void build_correctlyBuildsAPayPalAccount() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .clientMetadataId("correlation_id")
                .source("paypal-sdk");

        String json = paypalAccountBuilder.build();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY);
        JSONObject jsonMetadata = jsonObject.getJSONObject(PaymentMethodBuilder.METADATA_KEY);

        assertNull(jsonAccount.opt("details"));
        assertEquals("test_auth_code", jsonAccount.getString("consentCode"));
        assertEquals("correlation_id", jsonAccount.getString("correlationId"));
        assertEquals("custom", jsonMetadata.getString("integration"));
        assertEquals("paypal-sdk", jsonMetadata.getString("source"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void usesCorrectInfoForMetadata() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder()
                .source("paypal-app");

        String json = payPalAccountBuilder.build();
        JSONObject metadata = new JSONObject(json).getJSONObject(PaymentMethodBuilder.METADATA_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("paypal-app", metadata.getString("source"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void setsIntegrationMethod() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder().integration(
                "test-integration");

        String json = payPalAccountBuilder.build();
        JSONObject metadata = new JSONObject(json).getJSONObject(PaymentMethodBuilder.METADATA_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void includesValidateOptionWhenSet() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .validate(true);

        String json = paypalAccountBuilder.build();
        JSONObject builtAccount = new JSONObject(json).getJSONObject(PAYPAL_KEY);

        assertEquals(true, builtAccount.getJSONObject("options").getBoolean("validate"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void doesNotIncludeEmptyObjectsWhenSerializing() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder();

        String json = payPalAccountBuilder.build();
        JSONObject builtAccount = new JSONObject(json).getJSONObject(PAYPAL_KEY);

        assertFalse(builtAccount.keys().hasNext());
    }
}
