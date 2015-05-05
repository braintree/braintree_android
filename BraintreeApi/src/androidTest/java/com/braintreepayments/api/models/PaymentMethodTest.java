package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import com.braintreepayments.api.exceptions.ServerException;

import java.util.List;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;

public class PaymentMethodTest extends AndroidTestCase {

    public void testParsePaymentMethodsParsesAndroidPayCards() throws ServerException {
        String androidPayResponse =
                stringFromFixture(mContext, "payment_methods/android_pay_card_get_payment_methods_response.json");

        List<PaymentMethod> paymentMethods = PaymentMethod.parsePaymentMethods(androidPayResponse);

        assertEquals(1, paymentMethods.size());
        assertTrue(paymentMethods.get(0) instanceof AndroidPayCard);
        assertEquals("Google Wallet", paymentMethods.get(0).getTypeLabel());
        assertEquals("fake-android-pay-nonce", paymentMethods.get(0).getNonce());
        assertEquals("Android Pay", paymentMethods.get(0).getDescription());
        assertEquals("11", ((AndroidPayCard) paymentMethods.get(0)).getLastTwo());
    }
}
