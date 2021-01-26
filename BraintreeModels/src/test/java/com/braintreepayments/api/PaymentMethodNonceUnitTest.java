package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.braintreepayments.api.PaymentMethodNonce.parsePaymentMethodNonces;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodNonceUnitTest {

    @Test
    public void parsePaymentMethods_parsesCards() throws JSONException {
        List<PaymentMethodNonce> paymentMethodNonces =
                parsePaymentMethodNonces(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE);

        assertTrue(paymentMethodNonces.get(0) instanceof CardNonce);
        assertEquals("Visa", paymentMethodNonces.get(0).getTypeLabel());
        assertEquals("123456-12345-12345-a-adfa", paymentMethodNonces.get(0).getNonce());
        assertEquals("ending in ••11", paymentMethodNonces.get(0).getDescription());
        assertEquals("11", ((CardNonce) paymentMethodNonces.get(0)).getLastTwo());
        assertTrue(paymentMethodNonces.get(0).isDefault());
    }

    @Test
    public void parsePaymentMethods_parsesPayPal() throws JSONException {
        List<PaymentMethodNonce> paymentMethodNonces =
                parsePaymentMethodNonces(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE);

        assertTrue(paymentMethodNonces.get(1) instanceof PayPalAccountNonce);
        assertEquals("PayPal", paymentMethodNonces.get(1).getTypeLabel());
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", paymentMethodNonces.get(1).getNonce());
        assertEquals("with email paypalaccount@example.com", paymentMethodNonces.get(1).getDescription());
        assertFalse(paymentMethodNonces.get(1).isDefault());
    }

    @Test
    public void parsePaymentMethods_parsesVenmoAccountNonce() throws JSONException {
        List<PaymentMethodNonce> paymentMethodNonces =
                parsePaymentMethodNonces(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_RESPONSE);

        assertTrue(paymentMethodNonces.get(2) instanceof VenmoAccountNonce);
        assertEquals("Venmo", paymentMethodNonces.get(2).getTypeLabel());
        assertEquals("fake-venmo-nonce", paymentMethodNonces.get(2).getNonce());
        assertEquals("happy-venmo-joe", ((VenmoAccountNonce) paymentMethodNonces.get(2)).getUsername());
    }

    @Test
    public void parsePaymentMethods_doesNotParseGooglePaymentCardNonces() throws JSONException {
        List<PaymentMethodNonce> paymentMethodNonces =
                parsePaymentMethodNonces(Fixtures.PAYMENT_METHODS_GET_PAYMENT_METHODS_ANDROID_PAY_RESPONSE);

        assertEquals(0, paymentMethodNonces.size());
    }

    @Test
    public void parsePaymentMethod_parsesCards() throws JSONException {
        JSONObject card = new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD);

        PaymentMethodNonce paymentMethodNonce = PaymentMethodNonce.parsePaymentMethodNonces(card, CardNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof CardNonce);
        assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
    }

    @Test
    public void parsePaymentMethod_parsesPayPal() throws JSONException {
        JSONObject paypal = new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT);

        PaymentMethodNonce paymentMethodNonce = PaymentMethodNonce.parsePaymentMethodNonces(paypal, PayPalAccountNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
        assertEquals("with email paypalaccount@example.com", paymentMethodNonce.getDescription());
    }

    @Test
    public void parsePaymentMethod_parsesCardResponses() throws JSONException {
        JSONObject card = new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD);

        PaymentMethodNonce paymentMethodNonce = PaymentMethodNonce.parsePaymentMethodNonces(card, CardNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof CardNonce);
        assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
    }

    @Test
    public void parsePaymentMethod_parsesPayPalResponses() throws JSONException {
        JSONObject paypal = new JSONObject(Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE);

        PaymentMethodNonce paymentMethodNonce = PaymentMethodNonce.parsePaymentMethodNonces(paypal, PayPalAccountNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
        assertEquals("with email paypalaccount@example.com", paymentMethodNonce.getDescription());

        PayPalAccountNonce payPalAccountNonce = (PayPalAccountNonce) paymentMethodNonce;
        assertNotNull(payPalAccountNonce.getCreditFinancing());
        assertEquals(18, payPalAccountNonce.getCreditFinancing().getTerm());
    }
}
