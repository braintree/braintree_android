package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutNonceUnitTest {

    @Test
    public void fromJson_createsVisaCheckoutNonce() throws JSONException {
        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJson(
                stringFromFixture("payment_methods/visa_checkout_response.json"));

        assertEquals("11", visaCheckoutNonce.getLastTwo());
        assertEquals("Visa", visaCheckoutNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", visaCheckoutNonce.getNonce());
        assertEquals("ending in ••11", visaCheckoutNonce.getDescription());
        assertFalse(visaCheckoutNonce.isDefault());
        assertEquals("Visa Checkout", visaCheckoutNonce.getTypeLabel());

        assertNotNull(visaCheckoutNonce.getShippingAddress());
        assertEquals("BT", visaCheckoutNonce.getShippingAddress().getFirstName());

        assertNotNull(visaCheckoutNonce.getUserData());
        assertEquals("BT", visaCheckoutNonce.getUserData().getUserFirstName());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJson(
                stringFromFixture("payment_methods/visa_checkout_response.json"));

        Parcel parcel = Parcel.obtain();
        visaCheckoutNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutNonce actual = VisaCheckoutNonce.CREATOR.createFromParcel(parcel);

        assertEquals(visaCheckoutNonce.getNonce(), actual.getNonce());
        assertEquals(visaCheckoutNonce.getDescription(), actual.getDescription());
        assertEquals(visaCheckoutNonce.isDefault(), actual.isDefault());

        assertEquals(visaCheckoutNonce.getLastTwo(), actual.getLastTwo());
        assertEquals(visaCheckoutNonce.getCardType(), actual.getCardType());

        assertEquals(visaCheckoutNonce.getShippingAddress().getFirstName(),
                actual.getShippingAddress().getFirstName());
        assertEquals(visaCheckoutNonce.getShippingAddress().getLastName(),
                actual.getShippingAddress().getLastName());

        assertEquals(visaCheckoutNonce.getUserData().getUserFirstName(),
                actual.getUserData().getUserFirstName());
        assertEquals(visaCheckoutNonce.getUserData().getUserLastName(),
                actual.getUserData().getUserLastName());
    }
}
