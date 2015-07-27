package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.AndroidPay;
import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.exceptions.ServerException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PaymentMethodTest {

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethods_parsesCards() throws JSONException {
        String getPaymentMethodsResponse =
                stringFromFixture(getTargetContext(), "payment_methods/get_payment_methods_response.json");

        List<PaymentMethod> paymentMethods = PaymentMethod.parsePaymentMethods(getPaymentMethodsResponse);

        assertTrue(paymentMethods.get(0) instanceof Card);
        assertEquals("Visa", paymentMethods.get(0).getTypeLabel());
        assertEquals("123456-12345-12345-a-adfa", paymentMethods.get(0).getNonce());
        assertEquals("ending in ••11", paymentMethods.get(0).getDescription());
        assertEquals("11", ((Card) paymentMethods.get(0)).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethods_parsesPayPal() throws JSONException {
        String getPaymentMethodsResponse =
                stringFromFixture(getTargetContext(), "payment_methods/get_payment_methods_response.json");

        List<PaymentMethod> paymentMethods = PaymentMethod.parsePaymentMethods(getPaymentMethodsResponse);

        assertTrue(paymentMethods.get(1) instanceof PayPalAccount);
        assertEquals("PayPal", paymentMethods.get(1).getTypeLabel());
        assertEquals("aaaaaa-bbbbbbb-109934023-1", paymentMethods.get(1).getNonce());
        assertEquals("with email paypalaccount@example.com", paymentMethods.get(1).getDescription());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethods_parsesAndroidPayCards() throws JSONException {
        String getPaymentMethodsResponse =
                stringFromFixture(getTargetContext(), "payment_methods/get_payment_methods_response.json");

        List<PaymentMethod> paymentMethods = PaymentMethod.parsePaymentMethods(getPaymentMethodsResponse);

        assertTrue(paymentMethods.get(2) instanceof AndroidPayCard);
        assertEquals("Google Wallet", paymentMethods.get(2).getTypeLabel());
        assertEquals("fake-android-pay-nonce", paymentMethods.get(2).getNonce());
        assertEquals("Android Pay", paymentMethods.get(2).getDescription());
        assertEquals("11", ((AndroidPayCard) paymentMethods.get(2)).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesCards() throws JSONException {
        JSONObject card = new JSONObject(
                stringFromFixture(getTargetContext(), "payment_methods/visa_credit_card.json"));

        PaymentMethod paymentMethod = PaymentMethod.parsePaymentMethod(card, Card.PAYMENT_METHOD_TYPE);

        assertTrue(paymentMethod instanceof Card);
        assertEquals("11", ((Card) paymentMethod).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesPayPal() throws JSONException {
        JSONObject paypal = new JSONObject(
                stringFromFixture(getTargetContext(), "payment_methods/paypal_account.json"));

        PaymentMethod paymentMethod = PaymentMethod.parsePaymentMethod(paypal, PayPalAccount.PAYMENT_METHOD_TYPE);

        assertTrue(paymentMethod instanceof PayPalAccount);
        assertEquals("with email paypalaccount@example.com", paymentMethod.getDescription());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesAndroidPayCards() throws JSONException {
        JSONObject androidPayCard = new JSONObject(
                stringFromFixture(getTargetContext(), "payment_methods/android_pay_card.json"));

        PaymentMethod paymentMethod = PaymentMethod.parsePaymentMethod(androidPayCard, AndroidPayCard.PAYMENT_METHOD_TYPE);

        assertTrue(paymentMethod instanceof AndroidPayCard);
        assertEquals("11", ((AndroidPayCard) paymentMethod).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesCardResponses() throws JSONException {
        JSONObject card = new JSONObject(
                stringFromFixture(getTargetContext(), "payment_methods/visa_credit_card_response.json"));

        PaymentMethod paymentMethod = PaymentMethod.parsePaymentMethod(card, Card.PAYMENT_METHOD_TYPE);

        assertTrue(paymentMethod instanceof Card);
        assertEquals("11", ((Card) paymentMethod).getLastTwo());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesPayPalResponses() throws JSONException {
        JSONObject paypal = new JSONObject(
                stringFromFixture(getTargetContext(), "payment_methods/paypal_account_response.json"));

        PaymentMethod paymentMethod = PaymentMethod.parsePaymentMethod(paypal, PayPalAccount.PAYMENT_METHOD_TYPE);

        assertTrue(paymentMethod instanceof PayPalAccount);
        assertEquals("with email paypalaccount@example.com", paymentMethod.getDescription());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void parsePaymentMethod_parsesAndroidPayCardResponses() throws JSONException {
        JSONObject androidPayCard = new JSONObject(
                stringFromFixture(getTargetContext(), "payment_methods/android_pay_card_response.json"));

        PaymentMethod paymentMethod = PaymentMethod.parsePaymentMethod(androidPayCard, AndroidPayCard.PAYMENT_METHOD_TYPE);

        assertTrue(paymentMethod instanceof AndroidPayCard);
        assertEquals("11", ((AndroidPayCard) paymentMethod).getLastTwo());
    }
}
