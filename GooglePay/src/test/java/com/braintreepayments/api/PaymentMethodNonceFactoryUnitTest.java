package com.braintreepayments.api;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodNonceFactoryUnitTest {

    @Test
    public void getPaymentMethodNonce_returnsGooglePay() throws JSONException {
        String jsonString = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_CARD_RESPONSE;

        PaymentMethodNonce nonce = PaymentMethodNonceFactory.fromString(jsonString);

        assertTrue(nonce instanceof GooglePayCardNonce);
    }

    @Test
    public void getPaymentMethodNonce_returnsPayPalAccountNonce() throws JSONException {
        String jsonString = Fixtures.REPSONSE_GOOGLE_PAY_PAYPAL_ACCOUNT;

        PaymentMethodNonce nonce = PaymentMethodNonceFactory.fromString(jsonString);

        assertTrue(nonce instanceof PayPalAccountNonce);
    }
}
