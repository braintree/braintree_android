package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutShippingAddressUnitTest {

    private VisaCheckoutShippingAddress mVisaCheckoutShippingAddress;

    @Before
    public void setup() throws JSONException {
       JSONObject sampleShippingAddress = new JSONObject()
               .put("firstName", "firstName")
               .put("lastName", "lastName")
               .put("streetAddress", "streetAddress")
               .put("locality", "locality")
               .put("region", "region")
               .put("postalCode", "postalCode")
               .put("countryCode", "countryCode");

        mVisaCheckoutShippingAddress = new VisaCheckoutShippingAddress(sampleShippingAddress);
    }

    @Test
    public void fillsOutFromJson() {
        assertEquals("firstName", mVisaCheckoutShippingAddress.getFirstName());
        assertEquals("lastName", mVisaCheckoutShippingAddress.getLastName());
        assertEquals("streetAddress", mVisaCheckoutShippingAddress.getStreetAddress());
        assertEquals("locality", mVisaCheckoutShippingAddress.getLocality());
        assertEquals("region", mVisaCheckoutShippingAddress.getRegion());
        assertEquals("postalCode", mVisaCheckoutShippingAddress.getPostalCode());
        assertEquals("countryCode", mVisaCheckoutShippingAddress.getCountryCode());
    }

    @Test
    public void parcelsCorrectly() {
        Parcel parcel = Parcel.obtain();
        mVisaCheckoutShippingAddress.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutShippingAddress actual = VisaCheckoutShippingAddress.CREATOR.createFromParcel(parcel);

        assertEquals(mVisaCheckoutShippingAddress.getFirstName(), actual.getFirstName());
        assertEquals(mVisaCheckoutShippingAddress.getLastName(), actual.getLastName());
        assertEquals(mVisaCheckoutShippingAddress.getStreetAddress(), actual.getStreetAddress());
        assertEquals(mVisaCheckoutShippingAddress.getLocality(), actual.getLocality());
        assertEquals(mVisaCheckoutShippingAddress.getRegion(), actual.getRegion());
        assertEquals(mVisaCheckoutShippingAddress.getPostalCode(), actual.getPostalCode());
        assertEquals(mVisaCheckoutShippingAddress.getCountryCode(), actual.getCountryCode());
    }

    @Test
    public void toStringIsCorrect() {
        String expected = "VisaCheckoutShippingAddress{";
        expected += "mFirstName='firstName', ";
        expected += "mLastName='lastName', ";
        expected += "mStreetAddress='streetAddress', ";
        expected += "mLocality='locality', ";
        expected += "mRegion='region', ";
        expected += "mPostalCode='postalCode', ";
        expected += "mCountryCode='countryCode'";
        expected += "}";

        assertEquals(expected, mVisaCheckoutShippingAddress.toString());
    }
}
