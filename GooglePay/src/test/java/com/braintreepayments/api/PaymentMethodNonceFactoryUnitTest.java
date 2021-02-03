package com.braintreepayments.api;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodNonceFactoryUnitTest {

    @Test
    public void getPaymentMethodNonce_returnsGooglePayment() throws JSONException {
        String jsonString = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE;

        PaymentMethodNonce nonce = PaymentMethodNonceFactory.fromString(jsonString);

        assertTrue(nonce instanceof GooglePaymentCardNonce);
    }

    @Test
    public void getPaymentMethodNonce_returnsPayPalAccountNonce() throws JSONException {
        String jsonString = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE;

        PaymentMethodNonce nonce = PaymentMethodNonceFactory.fromString(jsonString);

        assertTrue(nonce instanceof PayPalAccountNonce);
    }
}
