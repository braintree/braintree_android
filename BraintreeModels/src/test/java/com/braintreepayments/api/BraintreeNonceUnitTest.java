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
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHOD_CARD);

        assertTrue(nonce.isDefault());
        assertEquals("123456-12345-12345-a-adfa", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHOD_CARD), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesGraphQLCardTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD);

        assertFalse(nonce.isDefault());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesRESTCardTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.TOKENIZE_CARD_SUCCESS_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.TOKENIZE_CARD_SUCCESS_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesPayPalTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.getNonce());
        assertEquals("with email paypalaccount@example.com", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesPayPalFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT);

        assertFalse(nonce.isDefault());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.getNonce());
        assertEquals("with email paypalaccount@example.com", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVenmoTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE);

        assertTrue(nonce.isDefault());
        assertEquals("fake-venmo-nonce", nonce.getNonce());
        assertEquals("VenmoAccount", nonce.getDescription());
        assertEquals("Venmo", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVenmoFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT);

        assertFalse(nonce.isDefault());
        assertEquals("fake-venmo-nonce", nonce.getNonce());
        assertEquals("VenmoAccount", nonce.getDescription());
        assertEquals("Venmo", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVisaCheckoutTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("123456-12345-12345-a-adfa", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa Checkout", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVisaCheckoutFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.VISA_CHECKOUT_NONCE_PLAIN_OBJECT);

        assertFalse(nonce.isDefault());
        assertEquals("123456-12345-12345-a-adfa", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa Checkout", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.VISA_CHECKOUT_NONCE_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesLocalPaymentTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", nonce.getNonce());
        assertEquals("PayPal", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesLocalPaymentPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.LOCAL_PAYMENT_PLAIN_OBJECT);

        assertFalse(nonce.isDefault());
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", nonce.getNonce());
        assertEquals("PayPal", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.LOCAL_PAYMENT_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesGooglePayTokenizeResponseJSON() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE);

        assertFalse(nonce.isDefault());
        assertEquals("fake-google-pay-nonce", nonce.getNonce());
        assertEquals("Google Pay", nonce.getDescription());
        assertEquals("Google Pay", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesGooglePayPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJson(Fixtures.GOOGLE_PAY_PLAIN_OBJECT);

        assertFalse(nonce.isDefault());
        assertEquals("fake-google-pay-nonce", nonce.getNonce());
        assertEquals("Google Pay", nonce.getDescription());
        assertEquals("Google Pay", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.GOOGLE_PAY_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }
}