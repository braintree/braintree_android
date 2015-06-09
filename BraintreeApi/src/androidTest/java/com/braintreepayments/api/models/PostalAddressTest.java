package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import com.braintreepayments.testutils.FixturesHelper;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class PostalAddressTest extends AndroidTestCase {

    public void testCanCreatePostalAddressFromJson() throws JSONException {
        String accountAddressJson = FixturesHelper.stringFromFixture(getContext(),
                "payment_methods/paypal_address.json");

        PostalAddress postalAddress = new Gson().fromJson(accountAddressJson, PostalAddress.class);

        assertEquals("123 Fake St.", postalAddress.getStreetAddress());
        assertEquals("Apt. 3", postalAddress.getExtendedAddress());
        assertEquals("Oakland", postalAddress.getLocality());
        assertEquals("CA", postalAddress.getRegion());
        assertEquals("94602", postalAddress.getPostalCode());
        assertEquals("US", postalAddress.getCountryCodeAlpha2());
    }

    public void testCanPostalAddressHandleMissingFieldsInJson() throws JSONException {
        String accountAddressJson = FixturesHelper.stringFromFixture(getContext(),
                "random_json.json");

        PostalAddress postalAddress = new Gson().fromJson(accountAddressJson, PostalAddress.class);

        assertNull(postalAddress.getStreetAddress());
        assertNull(postalAddress.getExtendedAddress());
        assertNull(postalAddress.getLocality());
        assertNull(postalAddress.getRegion());
        assertNull(postalAddress.getPostalCode());
        assertNull(postalAddress.getCountryCodeAlpha2());
    }
}
