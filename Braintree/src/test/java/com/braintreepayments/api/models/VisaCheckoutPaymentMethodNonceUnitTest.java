package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutPaymentMethodNonceUnitTest {

    @Test
    public void fromJson_createsVisaCheckoutPaymentMethodNonce() throws JSONException {
        VisaCheckoutPaymentMethodNonce paymentMethodNonce = VisaCheckoutPaymentMethodNonce.fromJson(
                stringFromFixture("payment_methods/visa_checkout_response.json"));

        assertEquals("11", paymentMethodNonce.getLastTwo());
        assertEquals("Visa", paymentMethodNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", paymentMethodNonce.getNonce());
        assertEquals("ending in ••11", paymentMethodNonce.getDescription());
        assertFalse(paymentMethodNonce.isDefault());
        assertEquals("Visa Checkout", paymentMethodNonce.getTypeLabel());

        assertNotNull(paymentMethodNonce.getShippingAddress());
        assertEquals("BT", paymentMethodNonce.getShippingAddress().getFirstName());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        VisaCheckoutPaymentMethodNonce expected = VisaCheckoutPaymentMethodNonce.fromJson(
                stringFromFixture("payment_methods/visa_checkout_response.json"));

        Parcel parcel = Parcel.obtain();
        expected.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutPaymentMethodNonce actual = VisaCheckoutPaymentMethodNonce.CREATOR.createFromParcel(parcel);

        assertEquals(expected.getNonce(), actual.getNonce());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.isDefault(), actual.isDefault());

        assertEquals(expected.getLastTwo(), actual.getLastTwo());
        assertEquals(expected.getCardType(), actual.getCardType());
    }
}
