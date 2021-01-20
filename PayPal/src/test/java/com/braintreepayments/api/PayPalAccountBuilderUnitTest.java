package com.braintreepayments.api;

import com.braintreepayments.api.models.MetadataBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalAccountBuilderUnitTest {

    private static final String PAYPAL_KEY = "paypalAccount";

    @Test
    public void build_correctlyBuildsAPayPalAccount() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .intent(PayPalRequest.INTENT_SALE)
                .clientMetadataId("correlation_id")
                .source("paypal-sdk")
                .merchantAccountId("alt_merchant_account_id");

        String json = paypalAccountBuilder.build();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY);
        JSONObject jsonMetadata = jsonObject.getJSONObject(MetadataBuilder.META_KEY);

        assertNull(jsonAccount.opt("details"));
        assertEquals("correlation_id", jsonAccount.getString("correlationId"));
        assertEquals(PayPalRequest.INTENT_SALE, jsonAccount.getString("intent"));
        assertEquals("custom", jsonMetadata.getString("integration"));
        assertEquals("paypal-sdk", jsonMetadata.getString("source"));
        assertEquals("alt_merchant_account_id", jsonObject.getString("merchant_account_id"));
    }

    @Test
    public void usesCorrectInfoForMetadata() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder()
                .source("paypal-app");

        String json = payPalAccountBuilder.build();
        JSONObject metadata = new JSONObject(json).getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("paypal-app", metadata.getString("source"));
    }

    @Test
    public void setsIntegrationMethod() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder().integration(
                "test-integration");

        String json = payPalAccountBuilder.build();
        JSONObject metadata = new JSONObject(json).getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    @Test
    public void includesValidateOptionWhenSet() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .validate(true);

        String json = paypalAccountBuilder.build();
        JSONObject builtAccount = new JSONObject(json).getJSONObject(PAYPAL_KEY);

        assertTrue(builtAccount.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void doesNotIncludeEmptyObjectsWhenSerializing() throws JSONException {
        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder();

        String json = payPalAccountBuilder.build();
        JSONObject builtAccount = new JSONObject(json).getJSONObject(PAYPAL_KEY);

        assertFalse(builtAccount.keys().hasNext());
    }

    @Test
    public void build_addsAllUrlResponseData() throws JSONException {
        JSONObject urlResponseData = new JSONObject()
                .put("data1", "data1")
                .put("data2", "data2")
                .put("data3", "data3");

        JSONObject base = new JSONObject();
        JSONObject paymentMethodNonceJson = new JSONObject();

        PayPalAccountBuilder payPalAccountBuilder = new PayPalAccountBuilder()
                .urlResponseData(urlResponseData);

        payPalAccountBuilder.build(base, paymentMethodNonceJson);

        JSONAssert.assertEquals(urlResponseData, paymentMethodNonceJson, JSONCompareMode.NON_EXTENSIBLE);
        JSONAssert.assertEquals(paymentMethodNonceJson, base.getJSONObject("paypalAccount"),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void build_doesNotIncludeIntentIfNotSet() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder();
        String json = paypalAccountBuilder.build();
        JSONObject jsonObject = new JSONObject(json);
        JSONObject jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY);

        assertFalse(jsonAccount.has("intent"));
    }
}
