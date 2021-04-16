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
        BraintreeNonce nonce = BraintreeNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHOD_CARD));

        assertTrue(nonce.isDefault());
        assertEquals(PaymentMethodType.CARD, nonce.getType());
        assertEquals("123456-12345-12345-a-adfa", nonce.getString());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa", nonce.getTypeLabel());
    }

    @Test
    public void constructor_parsesPayPalFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT));

        assertFalse(nonce.isDefault());
        assertEquals(PaymentMethodType.PAYPAL, nonce.getType());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", nonce.getString());
        assertEquals("with email paypalaccount@example.com", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
    }

    @Test
    public void constructor_parsesVenmoFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHOD_VENMO_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals(PaymentMethodType.VENMO, nonce.getType());
        assertEquals("fake-venmo-nonce", nonce.getString());
        assertEquals("VenmoAccount", nonce.getDescription());
        assertEquals("Venmo", nonce.getTypeLabel());
    }

    @Test
    public void constructor_parsesVisaCheckoutFromPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJSON(new JSONObject(Fixtures.VISA_CHECKOUT_NONCE_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals(PaymentMethodType.VISA_CHECKOUT, nonce.getType());
        assertEquals("123456-12345-12345-a-adfa", nonce.getString());
        assertEquals("ending in ••11", nonce.getDescription());
        assertEquals("Visa Checkout", nonce.getTypeLabel());
    }

    @Test
    public void constructor_parsesLocalPaymentPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJSON(new JSONObject(Fixtures.LOCAL_PAYMENT_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals(PaymentMethodType.PAYPAL, nonce.getType());
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", nonce.getString());
        assertEquals("PayPal", nonce.getDescription());
        assertEquals("PayPal", nonce.getTypeLabel());
    }

    @Test
    public void constructor_parsesGooglePayPlainJSONObject() throws JSONException {
        BraintreeNonce nonce = BraintreeNonce.fromJSON(new JSONObject(Fixtures.GOOGLE_PAY_PLAIN_OBJECT));

        assertFalse(nonce.isDefault());
        assertEquals(PaymentMethodType.GOOGLE_PAY, nonce.getType());
        assertEquals("fake-google-pay-nonce", nonce.getString());
        assertEquals("Google Pay", nonce.getDescription());
        assertEquals("Google Pay", nonce.getTypeLabel());
    }
}