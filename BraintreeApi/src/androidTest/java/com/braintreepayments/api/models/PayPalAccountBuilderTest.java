package com.braintreepayments.api.models;

import com.braintreepayments.api.models.PaymentMethod.Builder;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

public class PayPalAccountBuilderTest extends TestCase {

    private static final String PAYPAL_KEY = "paypal_account";

    public void testBuildsAPayPalAccountCorrectly() throws JSONException {
        JSONObject otcResponse = new JSONObject(
                "{\"client\":{\"environment\":\"OneTouchCore-Android\",\"paypal_sdk_version\":" +
                        "\"1.0.4\",\"platform\":\"Android\",\"product_name\":\"OneTouchCore-Android\"}," +
                        "\"response\":{\"code\":\"test_auth_code\"},\"response_type\":\"code\"," +
                        "\"user\":{\"display_string\":\"test_email\"}}");

        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .email("test_email")
                .correlationId("correlation_id")
                .OtcResponse(otcResponse)
                .source("paypal-sdk");

        JSONObject json = new JSONObject(paypalAccountBuilder.toJsonString());
        JSONObject jsonAccount = json.getJSONObject(PAYPAL_KEY);
        JSONObject jsonMetadata = json.getJSONObject(Builder.METADATA_KEY);

        assertNull(jsonAccount.opt("details"));
        assertEquals("test_auth_code", jsonAccount.getJSONObject("response").getString("code"));
        assertEquals("correlation_id", json.getString("correlation_id"));
        assertEquals("custom", jsonMetadata.getString("integration"));
        assertEquals("paypal-sdk", jsonMetadata.getString("source"));
    }

    public void testUsesCorrectInfoForMetadata() throws JSONException {
        JSONObject otcResponse = new JSONObject(
                "{\"client\":{\"environment\":\"OneTouchCore-Android\",\"paypal_sdk_version\":" +
                        "\"1.0.4\",\"platform\":\"Android\",\"product_name\":\"OneTouchCore-Android\"}," +
                        "\"response\":{\"code\":\"test_auth_code\"},\"response_type\":\"code\"," +
                        "\"user\":{\"display_string\":\"test_email\"}}");

        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .email("test_email")
                .correlationId("correlation_id")
                .OtcResponse(otcResponse)
                .source("paypal-app");

        JSONObject metadata = new JSONObject(paypalAccountBuilder.toJsonString()).getJSONObject(
                Builder.METADATA_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("paypal-app", metadata.getString("source"));
    }

    public void testSetsIntegrationMethod() throws JSONException {
        JSONObject otcResponse = new JSONObject(
                "{\"client\":{\"environment\":\"OneTouchCore-Android\",\"paypal_sdk_version\":" +
                        "\"1.0.4\",\"platform\":\"Android\",\"product_name\":\"OneTouchCore-Android\"}," +
                        "\"response\":{\"code\":\"test_auth_code\"},\"response_type\":\"code\"," +
                        "\"user\":{\"display_string\":\"test_email\"}}");

        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .integration("test-integration")
                .email("test_email")
                .correlationId("correlation_id")
                .OtcResponse(otcResponse)
                .source("paypal-sdk");

        JSONObject metadata = new JSONObject(paypalAccountBuilder.toJsonString())
                .getJSONObject(Builder.METADATA_KEY);

        assertEquals("test-integration", metadata.getString("integration"));
    }

    public void testIncludesValidateOptionWhenSet() throws JSONException {
        PayPalAccountBuilder paypalAccountBuilder = new PayPalAccountBuilder()
                .OtcResponse(new JSONObject())
                .validate(true);

        JSONObject builtAccount =
                new JSONObject(paypalAccountBuilder.toJsonString()).getJSONObject(PAYPAL_KEY);

        assertEquals(true, builtAccount.getJSONObject("options").getBoolean("validate"));
    }


}
