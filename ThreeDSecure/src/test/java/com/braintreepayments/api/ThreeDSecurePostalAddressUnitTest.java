package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecurePostalAddressUnitTest {

    @Test
    public void constructsCorrectly() {
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress();
        postalAddress.setStreetAddress("123 Fake St.");
        postalAddress.setExtendedAddress("Apt. 3");
        postalAddress.setLocality("Oakland");
        postalAddress.setRegion("CA");
        postalAddress.setPostalCode("94602");
        postalAddress.setCountryCodeAlpha2("US");
        postalAddress.setGivenName("John");
        postalAddress.setSurname("Fakerson");
        postalAddress.setPhoneNumber("5151231234");

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
        assertEquals("John", postalAddress.getGivenName());
        assertEquals("Fakerson", postalAddress.getSurname());
        assertEquals("5151231234", postalAddress.getPhoneNumber());
    }

    @Test
    public void testWriteToParcel_serializesCorrectly() {
        ThreeDSecurePostalAddress preSerialized = new ThreeDSecurePostalAddress();
        preSerialized.setStreetAddress("123 Fake St.");
        preSerialized.setExtendedAddress("Apt. 3");
        preSerialized.setLine3("Suite A");
        preSerialized.setLocality("Oakland");
        preSerialized.setRegion("CA");
        preSerialized.setPostalCode("94602");
        preSerialized.setCountryCodeAlpha2("US");
        preSerialized.setGivenName("John");
        preSerialized.setSurname("Fakerson");
        preSerialized.setPhoneNumber("5151231234");

        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecurePostalAddress postSerialized = ThreeDSecurePostalAddress.CREATOR.createFromParcel(parcel);

        assertNotNull(postSerialized);
        assertEquals("123 Fake St.", postSerialized.getStreetAddress());
        assertEquals("Apt. 3", postSerialized.getExtendedAddress());
        assertEquals("Suite A", postSerialized.getLine3());
        assertEquals("Oakland", postSerialized.getLocality());
        assertEquals("CA", postSerialized.getRegion());
        assertEquals("94602", postSerialized.getPostalCode());
        assertEquals("US", postSerialized.getCountryCodeAlpha2());
        assertEquals("John", postSerialized.getGivenName());
        assertEquals("Fakerson", postSerialized.getSurname());
        assertEquals("5151231234", postSerialized.getPhoneNumber());
    }

    @Test
    public void testToJson_buildsAllParameters() throws JSONException {
        ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();
        address.setStreetAddress("123 Fake St.");
        address.setExtendedAddress("Apt. 3");
        address.setLine3("Suite C");
        address.setLocality("Oakland");
        address.setRegion("CA");
        address.setPostalCode("94602");
        address.setCountryCodeAlpha2("US");
        address.setGivenName("John");
        address.setSurname("Fakerson");
        address.setPhoneNumber("5151231234");

        JSONObject jsonParams = address.toJson();
        JSONObject jsonBillingAddress = jsonParams.getJSONObject("billingAddress");

        assertEquals("John", jsonParams.get("firstName"));
        assertEquals("Fakerson", jsonParams.get("lastName"));
        assertEquals("5151231234", jsonParams.get("phoneNumber"));
        assertEquals("123 Fake St.", jsonBillingAddress.get("line1"));
        assertEquals("Apt. 3", jsonBillingAddress.get("line2"));
        assertEquals("Suite C", jsonBillingAddress.get("line3"));
        assertEquals("Oakland", jsonBillingAddress.get("city"));
        assertEquals("CA", jsonBillingAddress.get("state"));
        assertEquals("94602", jsonBillingAddress.get("postalCode"));
        assertEquals("US", jsonBillingAddress.get("countryCode"));
    }

    @Test
    public void testToJson_buildsPartialParameters() throws JSONException {
        ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();
        address.setStreetAddress("123 Fake St.");
        address.setExtendedAddress("Apt. 3");
        address.setLocality("Oakland");
        address.setRegion("CA");
        address.setPostalCode("94602");
        address.setGivenName("John");
        address.setSurname("Fakerson");

        JSONObject jsonParams = address.toJson();
        JSONObject jsonBillingAddress = jsonParams.getJSONObject("billingAddress");

        assertEquals("John", jsonParams.get("firstName"));
        assertEquals("Fakerson", jsonParams.get("lastName"));
        assertTrue(jsonParams.isNull("phoneNumber"));
        assertEquals("123 Fake St.", jsonBillingAddress.get("line1"));
        assertEquals("Apt. 3", jsonBillingAddress.get("line2"));
        assertEquals("Oakland", jsonBillingAddress.get("city"));
        assertEquals("CA", jsonBillingAddress.get("state"));
        assertEquals("94602", jsonBillingAddress.get("postalCode"));
        assertTrue(jsonBillingAddress.isNull("countryCode"));
    }

    @Test
    public void testToJson_buildsEmptyParameters() {
        ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress();

        JSONObject jsonParams = address.toJson();

        assertTrue(jsonParams.isNull("billingAddress"));
        assertTrue(jsonParams.isNull("firstName"));
        assertTrue(jsonParams.isNull("lastName"));
        assertTrue(jsonParams.isNull("phoneNumber"));
    }
}
