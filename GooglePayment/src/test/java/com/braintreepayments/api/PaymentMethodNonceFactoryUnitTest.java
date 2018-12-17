package com.braintreepayments.api;

import com.braintreepayments.api.models.GooglePaymentCardNonce;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PaymentMethodNonceFactory;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.test.FixturesHelper.stringFromFixture;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodNonceFactoryUnitTest {

    @Test
    public void getPaymentMethodNonce_returnsGooglePayment() throws JSONException {
        String jsonString = stringFromFixture("payment_methods/google_pay_card_response.json");

        PaymentMethodNonce nonce = PaymentMethodNonceFactory.fromString(jsonString);

        assertTrue(nonce instanceof GooglePaymentCardNonce);
    }

    @Test
    public void getPaymentMethodNonce_returnsPayPalAccountNonce() throws JSONException {
        String jsonString = stringFromFixture("payment_methods/paypal_account_response.json");

        PaymentMethodNonce nonce = PaymentMethodNonceFactory.fromString(jsonString);

        assertTrue(nonce instanceof PayPalAccountNonce);
    }
}
