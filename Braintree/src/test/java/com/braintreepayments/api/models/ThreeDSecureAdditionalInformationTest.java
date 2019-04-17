package com.braintreepayments.api.models;

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
public class ThreeDSecureAdditionalInformationTest {

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
                .lastName("Fakerson");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingPhoneNumber("billing-phone-number")
                .billingGivenName("billing-given-name")
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method");

        assertEquals("billing-phone-number", additionalInformation.getBillingPhoneNumber());
        assertEquals("billing-given-name", additionalInformation.getBillingGivenName());
        assertEquals("billing-surname", additionalInformation.getBillingSurname());
        assertEquals("email", additionalInformation.getEmail());
        assertEquals("shipping-method", additionalInformation.getShippingMethod());
        assertEquals(postalAddress, additionalInformation.getBillingAddress());
    }

    @Test
    public void testWriteToParcel_serializesCorrectly() throws JSONException {
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("street-address")
                .extendedAddress("extended-address")
                .countryCodeAlpha2("country-code");

        ThreeDSecureAdditionalInformation preSerialized = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingPhoneNumber("billing-phone-number")
                .billingGivenName("billing-given-name")
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method");

        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureAdditionalInformation postSerialized = ThreeDSecureAdditionalInformation.CREATOR.createFromParcel(parcel);

        assertNotNull(postSerialized);
        assertEquals("billing-phone-number", postSerialized.getBillingPhoneNumber());
        assertEquals("billing-given-name", postSerialized.getBillingGivenName());
        assertEquals("billing-surname", postSerialized.getBillingSurname());
        assertEquals("email", postSerialized.getEmail());
        assertEquals("shipping-method", postSerialized.getShippingMethod());
        assertEquals("country-code", postSerialized.getBillingAddress().getCountryCodeAlpha2());
        assertEquals("street-address", postSerialized.getBillingAddress().getStreetAddress());
        assertEquals("extended-address", postSerialized.getBillingAddress().getExtendedAddress());
    }

    @Test
    public void testToJson_buildsAllParameters() throws JSONException{
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("street-address")
                .extendedAddress("extended-address")
                .locality("locality");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingPhoneNumber("billing-phone-number")
                .billingGivenName("billing-given-name")
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method");

        JSONObject jsonParams = additionalInformation.toJson();

        assertEquals("billing-given-name", jsonParams.get("firstName"));
        assertEquals("billing-surname", jsonParams.get("lastName"));
        assertEquals("billing-phone-number", jsonParams.get("mobilePhoneNumber"));
        assertEquals("email", jsonParams.get("email"));
        assertEquals("shipping-method", jsonParams.get("shippingMethod"));
        assertEquals("street-address", jsonParams.get("line1"));
        assertEquals("extended-address", jsonParams.get("line2"));
        assertEquals("locality", jsonParams.get("city"));
    }

    @Test
    public void testToJson_buildsPartialParameters() throws JSONException{
        ThreeDSecurePostalAddress postalAddress = new ThreeDSecurePostalAddress()
                .streetAddress("street-address")
                .extendedAddress("extended-address")
                .locality("locality");

        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .billingAddress(postalAddress)
                .billingSurname("billing-surname")
                .email("email")
                .shippingMethod("shipping-method");

        JSONObject jsonParams = additionalInformation.toJson();

        assertTrue(jsonParams.isNull("firstName"));
        assertEquals("billing-surname", jsonParams.get("lastName"));
        assertTrue(jsonParams.isNull("mobilePhoneNumber"));
        assertEquals("street-address", jsonParams.get("line1"));
        assertEquals("extended-address", jsonParams.get("line2"));
        assertTrue(jsonParams.isNull("postalCode"));
        assertTrue(jsonParams.isNull("state"));
        assertTrue(jsonParams.isNull("countryCode"));
    }

    @Test
    public void testToJson_buildsEmptyParameters() throws JSONException{
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation();

        JSONObject jsonParams = additionalInformation.toJson();

        assertTrue(jsonParams.isNull("billingAddress"));
        assertTrue(jsonParams.isNull("firstName"));
        assertTrue(jsonParams.isNull("lastName"));
        assertTrue(jsonParams.isNull("shippingMethod"));
        assertTrue(jsonParams.isNull("mobilePhoneNumber"));
    }
}
