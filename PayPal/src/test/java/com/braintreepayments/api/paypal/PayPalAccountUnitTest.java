package com.braintreepayments.api.paypal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;

import com.braintreepayments.api.core.IntegrationType;
import com.braintreepayments.api.core.MetadataBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class PayPalAccountUnitTest {

    private static final String PAYPAL_KEY = "paypalAccount";

    @Test
    public void build_correctlyBuildsAPayPalAccount() throws JSONException {
        PayPalAccount sut = new PayPalAccount(
            "correlation_id",
            new JSONObject(),
            PayPalPaymentIntent.SALE,
            "alt_merchant_account_id",
            "single-payment",
                "session_id",
                "form",
                IntegrationType.CUSTOM
        );
        sut.setSource("paypal-sdk");

        JSONObject jsonObject = sut.buildJSON();
        JSONObject jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY);
        JSONObject jsonMetadata = jsonObject.getJSONObject(MetadataBuilder.META_KEY);

        assertNull(jsonAccount.opt("details"));
        assertEquals("correlation_id", jsonAccount.getString("correlationId"));
        assertEquals(PayPalPaymentIntent.SALE, PayPalPaymentIntent.fromString(jsonAccount.getString("intent")));
        assertEquals("custom", jsonMetadata.getString("integration"));
        assertEquals("paypal-sdk", jsonMetadata.getString("source"));
        assertEquals("alt_merchant_account_id", jsonObject.getString("merchant_account_id"));
        assertFalse(jsonAccount.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void usesCorrectInfoForMetadata() throws JSONException {
        PayPalAccount sut = new PayPalAccount(
            "correlation_id",
            new JSONObject(),
            PayPalPaymentIntent.SALE,
            "alt_merchant_account_id",
            "single-payment",
                "session_id",
                "form",
                IntegrationType.CUSTOM
        );
        sut.setSource("paypal-app");

        JSONObject json = sut.buildJSON();
        JSONObject metadata = json.getJSONObject(MetadataBuilder.META_KEY);

        assertEquals("custom", metadata.getString("integration"));
        assertEquals("paypal-app", metadata.getString("source"));
    }

    @Test
    public void setsIntegrationMethod() throws JSONException {
        PayPalAccount sut = new PayPalAccount(
            "correlation_id",
            new JSONObject(),
            PayPalPaymentIntent.SALE,
            "alt_merchant_account_id",
            "single-payment",
                "session_id",
                "form",
                IntegrationType.CUSTOM
        );
        sut.setIntegration(IntegrationType.CUSTOM);

        JSONObject json = sut.buildJSON();
        JSONObject metadata = json.getJSONObject(MetadataBuilder.META_KEY);

        assertEquals(IntegrationType.CUSTOM.getStringValue(), metadata.getString("integration"));
    }

    @Test
    public void buildJSON_whenPaymentTypeSinglePayment_setsOptionsValidateFalse()
        throws JSONException {
        PayPalAccount sut = new PayPalAccount(
            "correlation_id",
            new JSONObject(),
            PayPalPaymentIntent.SALE,
            "alt_merchant_account_id",
            "single-payment",
                "session_id",
                "form",
                IntegrationType.CUSTOM
        );

        JSONObject json = sut.buildJSON();
        JSONObject builtAccount = json.getJSONObject(PAYPAL_KEY);

        assertFalse(builtAccount.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void buildJSON_whenPaymentTypeNotSinglePayment_doesNotSetOptionsValidate()
        throws JSONException {
        PayPalAccount sut = new PayPalAccount(
            "correlation_id",
            new JSONObject(),
            PayPalPaymentIntent.SALE,
            "alt_merchant_account_id",
            "billing-agreement",
                "session_id",
                "form",
                IntegrationType.CUSTOM
        );

        JSONObject json = sut.buildJSON();
        JSONObject builtAccount = json.getJSONObject(PAYPAL_KEY);

        assertFalse(builtAccount.has("options"));
    }

    @Test
    public void doesNotIncludeEmptyObjectsWhenSerializing() throws JSONException {
        PayPalAccount sut = new PayPalAccount(
            null,
            new JSONObject(),
            null,
            null,
            null,
                null,
                null,
                null
        );

        JSONObject json = sut.buildJSON();
        JSONObject builtAccount = json.getJSONObject(PAYPAL_KEY);

        assertEquals(0, builtAccount.length());
    }

    @Test
    public void build_addsAllUrlResponseData() throws JSONException {
        JSONObject urlResponseData = new JSONObject()
            .put("data1", "data1")
            .put("data2", "data2")
            .put("data3", "data3");

        PayPalAccount sut = new PayPalAccount(
            null,
            urlResponseData,
            null,
            null,
            null,
                null,
                null,
                null
        );

        JSONObject json = sut.buildJSON();
        JSONObject paymentMethodNonceJson = json.getJSONObject(PAYPAL_KEY);

        JSONObject expectedPaymentMethodNonceJSON = new JSONObject()
            .put("data1", "data1")
            .put("data2", "data2")
            .put("data3", "data3");
        JSONAssert.assertEquals(expectedPaymentMethodNonceJSON, paymentMethodNonceJson,
            JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void build_doesNotIncludeIntentIfNotSet() throws JSONException {
        PayPalAccount sut = new PayPalAccount(
            null,
            new JSONObject(),
            null,
            null,
            null,
                null,
                null,
                null
        );

        JSONObject jsonObject = sut.buildJSON();
        JSONObject jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY);

        assertFalse(jsonAccount.has("intent"));
    }
}
