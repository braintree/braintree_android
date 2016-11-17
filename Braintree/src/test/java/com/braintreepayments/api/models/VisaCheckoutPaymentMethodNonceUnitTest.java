package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutPaymentMethodNonceUnitTest {
    VisaCheckoutPaymentMethodNonce  mVisaCheckoutPaymentMethodNonce;

    @Before
    public void setup() throws JSONException {
        mVisaCheckoutPaymentMethodNonce = VisaCheckoutPaymentMethodNonce.fromJson(
                stringFromFixture("payment_methods/visa_checkout_response.json"));
    }

    @Test
    public void fromJson_createsVisaCheckoutPaymentMethodNonce() {
        assertEquals("11", mVisaCheckoutPaymentMethodNonce.getLastTwo());
        assertEquals("Visa", mVisaCheckoutPaymentMethodNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", mVisaCheckoutPaymentMethodNonce.getNonce());
        assertEquals("ending in ••11", mVisaCheckoutPaymentMethodNonce.getDescription());
        assertFalse(mVisaCheckoutPaymentMethodNonce.isDefault());
        assertEquals("Visa Checkout", mVisaCheckoutPaymentMethodNonce.getTypeLabel());

        assertNotNull(mVisaCheckoutPaymentMethodNonce.getShippingAddress());
        assertEquals("BT", mVisaCheckoutPaymentMethodNonce.getShippingAddress().getFirstName());

        assertNotNull(mVisaCheckoutPaymentMethodNonce.getUserData());
        assertEquals("BT", mVisaCheckoutPaymentMethodNonce.getUserData().getUserFirstName());
    }

    @Test
    public void parcelsCorrectly() {
        Parcel parcel = Parcel.obtain();
        mVisaCheckoutPaymentMethodNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutPaymentMethodNonce actual = VisaCheckoutPaymentMethodNonce.CREATOR.createFromParcel(parcel);

        assertEquals(mVisaCheckoutPaymentMethodNonce.getNonce(), actual.getNonce());
        assertEquals(mVisaCheckoutPaymentMethodNonce.getDescription(), actual.getDescription());
        assertEquals(mVisaCheckoutPaymentMethodNonce.isDefault(), actual.isDefault());

        assertEquals(mVisaCheckoutPaymentMethodNonce.getLastTwo(), actual.getLastTwo());
        assertEquals(mVisaCheckoutPaymentMethodNonce.getCardType(), actual.getCardType());

        assertEquals(mVisaCheckoutPaymentMethodNonce.getShippingAddress().getFirstName(),
                actual.getShippingAddress().getFirstName());
        assertEquals(mVisaCheckoutPaymentMethodNonce.getShippingAddress().getLastName(),
                actual.getShippingAddress().getLastName());

        assertEquals(mVisaCheckoutPaymentMethodNonce.getUserData().getUserFirstName(),
                actual.getUserData().getUserFirstName());
        assertEquals(mVisaCheckoutPaymentMethodNonce.getUserData().getUserLastName(),
                actual.getUserData().getUserLastName());
    }

    @Test
    public void toStringIsCorrect() {
        String expected = "VisaCheckoutPaymentMethodNonce{";
        expected += "mLastTwo='11', ";
        expected += "mCardType='Visa', ";
        expected += String.format("mShippingAddress=%s, ", mVisaCheckoutPaymentMethodNonce.getShippingAddress());
        expected += String.format("mUserData=%s", mVisaCheckoutPaymentMethodNonce.getUserData());
        expected += '}';

        assertEquals(expected, mVisaCheckoutPaymentMethodNonce.toString());
    }
}
