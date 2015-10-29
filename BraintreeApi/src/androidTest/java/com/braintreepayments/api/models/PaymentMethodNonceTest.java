package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.braintreepayments.api.models.PaymentMethodNonce.parsePaymentMethodNonces;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PaymentMethodNonceTest {

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethods_parsesCards() throws JSONException {
        String getPaymentMethodsResponse =
                stringFromFixture("payment_methods/get_payment_methods_response.json");

        List<PaymentMethodNonce> paymentMethodNonces =
                parsePaymentMethodNonces(getPaymentMethodsResponse);

        assertTrue(paymentMethodNonces.get(0) instanceof CardNonce);
        assertEquals("Visa", paymentMethodNonces.get(0).getTypeLabel());
        assertEquals("123456-12345-12345-a-adfa", paymentMethodNonces.get(0).getNonce());
        assertEquals("ending in ••11", paymentMethodNonces.get(0).getDescription());
        assertEquals("11", ((CardNonce) paymentMethodNonces.get(0)).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethods_parsesPayPal() throws JSONException {
        String getPaymentMethodsResponse =
                stringFromFixture("payment_methods/get_payment_methods_response.json");

        List<PaymentMethodNonce> paymentMethodNonces =
                parsePaymentMethodNonces(getPaymentMethodsResponse);

        assertTrue(paymentMethodNonces.get(1) instanceof PayPalAccountNonce);
        assertEquals("PayPal", paymentMethodNonces.get(1).getTypeLabel());
        assertEquals("aaaaaa-bbbbbbb-109934023-1", paymentMethodNonces.get(1).getNonce());
        assertEquals("with email paypalaccount@example.com",
                paymentMethodNonces.get(1).getDescription());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethods_parsesAndroidPayCardNonces() throws JSONException {
        String getPaymentMethodsResponse =
                stringFromFixture("payment_methods/get_payment_methods_response.json");

        List<PaymentMethodNonce> paymentMethodNonces =
                parsePaymentMethodNonces(getPaymentMethodsResponse);

        assertTrue(paymentMethodNonces.get(2) instanceof AndroidPayCardNonce);
        assertEquals("Google Wallet", paymentMethodNonces.get(2).getTypeLabel());
        assertEquals("fake-android-pay-nonce", paymentMethodNonces.get(2).getNonce());
        assertEquals("Android Pay", paymentMethodNonces.get(2).getDescription());
        assertEquals("11", ((AndroidPayCardNonce) paymentMethodNonces.get(2)).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesCards() throws JSONException {
        JSONObject card = new JSONObject(
                stringFromFixture("payment_methods/visa_credit_card.json"));

        PaymentMethodNonce paymentMethodNonce =
                parsePaymentMethodNonces(card, CardNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof CardNonce);
        assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesPayPal() throws JSONException {
        JSONObject paypal = new JSONObject(
                stringFromFixture("payment_methods/paypal_account.json"));

        PaymentMethodNonce paymentMethodNonce =
                parsePaymentMethodNonces(paypal, PayPalAccountNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
        assertEquals("with email paypalaccount@example.com", paymentMethodNonce.getDescription());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesAndroidPayCardNonces() throws JSONException {
        JSONObject androidPayCard = new JSONObject(
                stringFromFixture("payment_methods/android_pay_card.json"));

        PaymentMethodNonce paymentMethodNonce =
                parsePaymentMethodNonces(androidPayCard, AndroidPayCardNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof AndroidPayCardNonce);
        assertEquals("11", ((AndroidPayCardNonce) paymentMethodNonce).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesCardResponses() throws JSONException {
        JSONObject card = new JSONObject(
                stringFromFixture("payment_methods/visa_credit_card_response.json"));

        PaymentMethodNonce paymentMethodNonce =
                parsePaymentMethodNonces(card, CardNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof CardNonce);
        assertEquals("11", ((CardNonce) paymentMethodNonce).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesPayPalResponses() throws JSONException {
        JSONObject paypal = new JSONObject(
                stringFromFixture("payment_methods/paypal_account_response.json"));

        PaymentMethodNonce paymentMethodNonce =
                parsePaymentMethodNonces(paypal, PayPalAccountNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof PayPalAccountNonce);
        assertEquals("with email paypalaccount@example.com", paymentMethodNonce.getDescription());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesAndroidPayCardResponses() throws JSONException {
        JSONObject androidPayCard = new JSONObject(
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        PaymentMethodNonce paymentMethodNonce =
                parsePaymentMethodNonces(androidPayCard, AndroidPayCardNonce.TYPE);

        assertTrue(paymentMethodNonce instanceof AndroidPayCardNonce);
        assertEquals("11", ((AndroidPayCardNonce) paymentMethodNonce).getLastTwo());
    }
}
