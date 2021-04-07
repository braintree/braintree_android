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
        BraintreeNonce nonce = new BraintreeNonce(new JSONObject(Fixtures.PAYMENT_METHOD_CARD));

        assertTrue(nonce.isDefault());
        assertEquals("123456-12345-12345-a-adfa", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHOD_CARD), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesPayPalFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT));

        assertFalse(nonce.isDefault());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.getNonce());
        assertEquals("with email paypalaccount@example.com", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVenmoFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(new JSONObject(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals("fake-venmo-nonce", nonce.getNonce());
        assertEquals("VenmoAccount", nonce.getDescription());
        assertEquals("Venmo", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesVisaCheckoutFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(new JSONObject(Fixtures.VISA_CHECKOUT_NONCE_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals("123456-12345-12345-a-adfa", nonce.getNonce());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa Checkout", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.VISA_CHECKOUT_NONCE_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesLocalPaymentPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(new JSONObject(Fixtures.LOCAL_PAYMENT_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", nonce.getNonce());
        assertEquals("PayPal", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.LOCAL_PAYMENT_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }

    @Test
    public void constructor_parsesGooglePayPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = new BraintreeNonce(new JSONObject(Fixtures.GOOGLE_PAY_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals("fake-google-pay-nonce", nonce.getNonce());
        assertEquals("Google Pay", nonce.getDescription());
        assertEquals("Google Pay", nonce.getTypeLabel());
        JSONAssert.assertEquals(new JSONObject(Fixtures.GOOGLE_PAY_PLAIN_OBJECT), new JSONObject(nonce.getJson()), true);
    }
}