package com.braintreepayments.api;

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
public class PayPalNativeCheckoutAccountUnitTest {

    private static final String PAYPAL_KEY = "paypalAccount";

    @Test
    public void build_correctlyBuildsAPayPalAccount() throws JSONException {
        JSONObject client = new JSONObject();
        PayPalNativeCheckoutAccount sut = new PayPalNativeCheckoutAccount();
        sut.setClientMetadataId("correlation_id");
        sut.setSource("paypal-sdk");
        sut.setMerchantAccountId("alt_merchant_account_id");
        sut.setPaymentType("single-payment");
        sut.setUrlResponseData(getUrlResponseData());
        sut.setClient(client);

        JSONObject jsonObject = sut.buildJSON();
        JSONObject jsonAccount = jsonObject.getJSONObject(PAYPAL_KEY);

        assertNull(jsonAccount.opt("details"));
        assertEquals("correlation_id", jsonAccount.getString("correlationId"));
        assertEquals("fake-url", jsonAccount.getJSONObject("response").getJSONObject("webURL").getString("webURL"));
        assertEquals(client, jsonAccount.getJSONObject("client"));
        assertEquals("alt_merchant_account_id", jsonObject.getString("merchant_account_id"));
        assertFalse(jsonAccount.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void buildJSON_whenPaymentTypeSinglePayment_setsOptionsValidateFalse() throws JSONException {
        PayPalNativeCheckoutAccount sut = new PayPalNativeCheckoutAccount();
        sut.setPaymentType("single-payment");
        sut.setUrlResponseData(getUrlResponseData());

        JSONObject json = sut.buildJSON();
        JSONObject builtAccount = json.getJSONObject(PAYPAL_KEY);

        assertFalse(builtAccount.getJSONObject("options").getBoolean("validate"));
    }

    @Test
    public void buildJSON_whenPaymentTypeNotSinglePayment_doesNotSetOptionsValidate() throws JSONException {
        PayPalNativeCheckoutAccount sut = new PayPalNativeCheckoutAccount();
        sut.setPaymentType("billing-agreement");
        sut.setUrlResponseData(getUrlResponseData());

        JSONObject json = sut.buildJSON();
        JSONObject builtAccount = json.getJSONObject(PAYPAL_KEY);

        assertFalse(builtAccount.has("options"));
    }

    private JSONObject getUrlResponseData() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        JSONObject webUrl = new JSONObject();
        webUrl.put("webURL", "fake-url");
        jsonObject.put("response", webUrl);

        return jsonObject;
    }
}
