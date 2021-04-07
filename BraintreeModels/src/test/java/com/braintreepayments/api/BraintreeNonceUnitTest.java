package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class BraintreeNonceUnitTest {

    @Test
    public void constructor_parsesCardFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.PAYMENT_METHOD_CARD);

        assertTrue(nonce.isDefault());
        assertEquals("123456-12345-12345-a-adfa", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHOD_CARD), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesGraphQLCardTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD);

        assertFalse(nonce.isDefault());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesRESTCardTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.TOKENIZE_CARD_SUCCESS_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.TOKENIZE_CARD_SUCCESS_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesPayPalTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.getNonce());
        assertEquals("with email paypalaccount@example.com", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesPayPalFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT);

        assertFalse(nonce.isDefault());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.getNonce());
        assertEquals("with email paypalaccount@example.com", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVenmoTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE);

        assertTrue(nonce.isDefault());
        assertEquals("fake-venmo-nonce", nonce.getNonce());
        assertEquals("VenmoAccount", nonce.getDescription());
        assertEquals("Venmo", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVenmoFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT);

        assertFalse(nonce.isDefault());
        assertEquals("fake-venmo-nonce", nonce.getNonce());
        assertEquals("VenmoAccount", nonce.getDescription());
        assertEquals("Venmo", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVisaCheckoutTokenizeResponseJSON() {

    }

    @Test
    public void constructor_parsesVisaCheckoutFromPlainJSONObject() {

    }
}