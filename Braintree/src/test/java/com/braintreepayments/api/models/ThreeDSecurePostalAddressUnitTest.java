package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecurePostalAddressUnitTest {

    @Test
    public void constructsCorrectly() throws JSONException {
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. 3")
                .locality("Oakland")
                .region("CA")
                .postalCode("94602")
                .countryCodeAlpha2("US")
                .firstName("John")
                .lastName("Fakerson")
                .phoneNumber("5151231234");

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
        assertEquals("John", postalAddress.getFirstName());
        assertEquals("Fakerson", postalAddress.getLastName());
        assertEquals("5151231234", postalAddress.getPhoneNumber());
    }

    @Test
    public void testWriteToParcel_serializesCorrectly() throws JSONException {
        ThreeDSecurePostalAddress preSerialized = new ThreeDSecurePostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. 3")
                .locality("Oakland")
                .region("CA")
                .postalCode("94602")
                .countryCodeAlpha2("US")
                .firstName("John")
                .lastName("Fakerson")
                .phoneNumber("5151231234");

        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecurePostalAddress postSerialized = ThreeDSecurePostalAddress.CREATOR.createFromParcel(parcel);

        assertNotNull(postSerialized);
        assertEquals("123 Fake St.", postSerialized.getStreetAddress());
        assertEquals("Apt. 3", postSerialized.getExtendedAddress());
        assertEquals("Oakland", postSerialized.getLocality());
        assertEquals("CA", postSerialized.getRegion());
        assertEquals("94602", postSerialized.getPostalCode());
        assertEquals("US", postSerialized.getCountryCodeAlpha2());
        assertEquals("John", postSerialized.getFirstName());
        assertEquals("Fakerson", postSerialized.getLastName());
        assertEquals("5151231234", postSerialized.getPhoneNumber());
    }
}
