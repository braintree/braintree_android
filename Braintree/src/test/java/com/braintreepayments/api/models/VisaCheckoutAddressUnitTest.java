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

    private JSONObject mSampleAddress;

    @Before
    public void setup() throws JSONException {
       mSampleAddress = new JSONObject()
               .put("firstName", "firstName")
               .put("lastName", "lastName")
               .put("streetAddress", "streetAddress")
               .put("extendedAddress", "extendedAddress")
               .put("locality", "locality")
               .put("region", "region")
               .put("postalCode", "postalCode")
               .put("countryCode", "countryCode")
               .put("phoneNumber", "phoneNumber");
    }

    @Test
    public void fromJson_whenValid_returnsPopulatedObject() {
        VisaCheckoutAddress visaCheckoutAddress = VisaCheckoutAddress.fromJson(mSampleAddress);

        assertEquals("firstName", visaCheckoutAddress.getFirstName());
        assertEquals("lastName", visaCheckoutAddress.getLastName());
        assertEquals("streetAddress", visaCheckoutAddress.getStreetAddress());
        assertEquals("extendedAddress", visaCheckoutAddress.getExtendedAddress());
        assertEquals("locality", visaCheckoutAddress.getLocality());
        assertEquals("region", visaCheckoutAddress.getRegion());
        assertEquals("postalCode", visaCheckoutAddress.getPostalCode());
        assertEquals("countryCode", visaCheckoutAddress.getCountryCode());
        assertEquals("phoneNumber", visaCheckoutAddress.getPhoneNumber());
    }

    @Test
    public void fromJson_whenNull_returnsEmptyObject() {
        VisaCheckoutAddress visaCheckoutAddress = VisaCheckoutAddress.fromJson(null);

        assertEquals("", visaCheckoutAddress.getFirstName());
        assertEquals("", visaCheckoutAddress.getLastName());
        assertEquals("", visaCheckoutAddress.getStreetAddress());
        assertEquals("", visaCheckoutAddress.getExtendedAddress());
        assertEquals("", visaCheckoutAddress.getLocality());
        assertEquals("", visaCheckoutAddress.getRegion());
        assertEquals("", visaCheckoutAddress.getPostalCode());
        assertEquals("", visaCheckoutAddress.getCountryCode());
        assertEquals("", visaCheckoutAddress.getPhoneNumber());
    }

    @Test
    public void parcelsCorrectly() {
        VisaCheckoutAddress visaCheckoutAddress = VisaCheckoutAddress.fromJson(mSampleAddress);

        Parcel parcel = Parcel.obtain();
        visaCheckoutAddress.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutAddress actual = VisaCheckoutAddress.CREATOR.createFromParcel(parcel);

        assertEquals(visaCheckoutAddress.getFirstName(), actual.getFirstName());
        assertEquals(visaCheckoutAddress.getLastName(), actual.getLastName());
        assertEquals(visaCheckoutAddress.getStreetAddress(), actual.getStreetAddress());
        assertEquals(visaCheckoutAddress.getExtendedAddress(), actual.getExtendedAddress());
        assertEquals(visaCheckoutAddress.getLocality(), actual.getLocality());
        assertEquals(visaCheckoutAddress.getRegion(), actual.getRegion());
        assertEquals(visaCheckoutAddress.getPostalCode(), actual.getPostalCode());
        assertEquals(visaCheckoutAddress.getCountryCode(), actual.getCountryCode());
        assertEquals(visaCheckoutAddress.getPhoneNumber(), actual.getPhoneNumber());
    }
}
