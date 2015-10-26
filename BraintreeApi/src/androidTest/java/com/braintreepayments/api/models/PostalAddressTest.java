package com.braintreepayments.api.models;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class PostalAddressTest {

    @Test(timeout = 1000)
    @SmallTest
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

    @Test(timeout = 10000)
    @MediumTest
    public void testCanCreatePostalAddress_fromStandardJson() throws JSONException {
        String accountAddressJson = stringFromFixture(
                "payment_methods/paypal_address.json");

        PostalAddress postalAddress = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
        assertEquals("John Fakerson", postalAddress.getRecipientName());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void testCanCreatePostalAddress_fromAlternateJson() throws JSONException {
        String accountAddressJson = stringFromFixture(
                "payment_methods/paypal_address_alternate.json");

        PostalAddress postalAddress = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
        assertEquals("John Fakerson", postalAddress.getRecipientName());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void testCanPostalAddressHandleMissingFieldsInJson() throws JSONException {
        String accountAddressJson = stringFromFixture(
                "random_json.json");

        PostalAddress postalAddress = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        assertNull(postalAddress.getStreetAddress());
        assertNull(postalAddress.getExtendedAddress());
        assertNull(postalAddress.getLocality());
        assertNull(postalAddress.getRegion());
        assertNull(postalAddress.getPostalCode());
        assertNull(postalAddress.getCountryCodeAlpha2());
    }

    @Test(timeout = 10000)
    @MediumTest
    public void testWriteToParcel_serializesCorrectly() throws JSONException{
        String accountAddressJson = stringFromFixture(
                "payment_methods/paypal_address.json");

        PostalAddress preSerialized = PostalAddress.fromJson(new JSONObject(accountAddressJson));

        Parcel p = Parcel.obtain();
        preSerialized.writeToParcel(p, 0);
        p.setDataPosition(0);

        PostalAddress postSerialized = PostalAddress.CREATOR.createFromParcel(p);

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

