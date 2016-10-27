package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class PostalAddressUnitTest {

    @Test
    public void constructsCorrectly() throws JSONException {
        PostalAddress postalAddress = new PostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. 3")
                .locality("Oakland")
                .region("CA")
                .postalCode("94602")
                .countryCodeAlpha2("US")
                .recipientName("John Fakerson");

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
        assertEquals("John Fakerson", postalAddress.getRecipientName());
    }

    @Test
    public void testCanCreatePostalAddress_fromStandardJson() throws JSONException {
        String accountAddressJson = stringFromFixture("payment_methods/paypal_address.json");

        PostalAddress postalAddress = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
        assertEquals("John Fakerson", postalAddress.getRecipientName());
    }

    @Test
    public void testCanCreatePostalAddress_fromAlternateJson() throws JSONException {
        String accountAddressJson = stringFromFixture("payment_methods/paypal_address_alternate.json");

        PostalAddress postalAddress = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
        assertEquals("John Fakerson", postalAddress.getRecipientName());
    }

    @Test
    public void testCanPostalAddressHandleMissingFieldsInJson() throws JSONException {
        String accountAddressJson = stringFromFixture("random_json.json");

        PostalAddress postalAddress = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        assertNull(postalAddress.getStreetAddress());
        assertNull(postalAddress.getExtendedAddress());
        assertNull(postalAddress.getLocality());
        assertNull(postalAddress.getRegion());
        assertNull(postalAddress.getPostalCode());
        assertNull(postalAddress.getCountryCodeAlpha2());
    }

    @Test
    public void testWriteToParcel_serializesCorrectly() throws JSONException{
        String accountAddressJson = stringFromFixture("payment_methods/paypal_address.json");

        PostalAddress preSerialized = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PostalAddress postSerialized = PostalAddress.CREATOR.createFromParcel(parcel);

        assertNotNull(postSerialized);
        assertEquals("123 Fake St.", postSerialized.getStreetAddress());
        assertEquals("Apt. 3", postSerialized.getExtendedAddress());
        assertEquals("Oakland", postSerialized.getLocality());
        assertEquals("CA", postSerialized.getRegion());
        assertEquals("94602", postSerialized.getPostalCode());
        assertEquals("US", postSerialized.getCountryCodeAlpha2());
        assertEquals("John Fakerson", postSerialized.getRecipientName());
    }
}
