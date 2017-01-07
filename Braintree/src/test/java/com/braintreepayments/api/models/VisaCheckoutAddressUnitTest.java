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
public class VisaCheckoutAddressUnitTest {

    private VisaCheckoutAddress mVisaCheckoutAddress;

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

        mVisaCheckoutAddress = new VisaCheckoutAddress(sampleShippingAddress);
    }

    @Test
    public void fillsOutFromJson() {
        assertEquals("firstName", mVisaCheckoutAddress.getFirstName());
        assertEquals("lastName", mVisaCheckoutAddress.getLastName());
        assertEquals("streetAddress", mVisaCheckoutAddress.getStreetAddress());
        assertEquals("locality", mVisaCheckoutAddress.getLocality());
        assertEquals("region", mVisaCheckoutAddress.getRegion());
        assertEquals("postalCode", mVisaCheckoutAddress.getPostalCode());
        assertEquals("countryCode", mVisaCheckoutAddress.getCountryCode());
    }

    @Test
    public void parcelsCorrectly() {
        Parcel parcel = Parcel.obtain();
        mVisaCheckoutAddress.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutAddress actual = VisaCheckoutAddress.CREATOR.createFromParcel(parcel);

        assertEquals(mVisaCheckoutAddress.getFirstName(), actual.getFirstName());
        assertEquals(mVisaCheckoutAddress.getLastName(), actual.getLastName());
        assertEquals(mVisaCheckoutAddress.getStreetAddress(), actual.getStreetAddress());
        assertEquals(mVisaCheckoutAddress.getLocality(), actual.getLocality());
        assertEquals(mVisaCheckoutAddress.getRegion(), actual.getRegion());
        assertEquals(mVisaCheckoutAddress.getPostalCode(), actual.getPostalCode());
        assertEquals(mVisaCheckoutAddress.getCountryCode(), actual.getCountryCode());
    }

    @Test
    public void toStringIsCorrect() {
        String expected = "VisaCheckoutAddress{";
        expected += "mFirstName='firstName', ";
        expected += "mLastName='lastName', ";
        expected += "mStreetAddress='streetAddress', ";
        expected += "mLocality='locality', ";
        expected += "mRegion='region', ";
        expected += "mPostalCode='postalCode', ";
        expected += "mCountryCode='countryCode'";
        expected += "}";

        assertEquals(expected, mVisaCheckoutAddress.toString());
    }
}
