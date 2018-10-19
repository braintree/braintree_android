package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureRequestUnitTest {

    @Test
    public void setsValuesCorrectly() {
        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress()
            .firstName("Joe")
            .lastName("Guy")
            .phoneNumber("12345678")
            .streetAddress("555 Smith St.")
            .extendedAddress("#5")
            .locality("Oakland")
            .region("CA")
            .countryCodeAlpha2("US")
            .postalCode("54321");

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .mobilePhoneNumber("5151234321")
                .email("tester@example.com")
                .shippingMethod("03")
                .billingAddress(billingAddress);

        assertEquals("1.00", request.getAmount());
        assertEquals("a-nonce", request.getNonce());
        assertEquals("5151234321", request.getMobilePhoneNumber());
        assertEquals("tester@example.com", request.getEmail());
        assertEquals("03", request.getShippingMethod());
        assertEquals(billingAddress, request.getBillingAddress());
    }

    @Test
    public void testWriteToParcel_serializesCorrectly() {
        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress()
                .firstName("Joe")
                .lastName("Guy")
                .phoneNumber("12345678")
                .streetAddress("555 Smith St.")
                .extendedAddress("#5")
                .locality("Oakland")
                .region("CA")
                .countryCodeAlpha2("US")
                .postalCode("54321");

        ThreeDSecureRequest expected = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .mobilePhoneNumber("5151234321")
                .email("tester@example.com")
                .shippingMethod("03")
                .billingAddress(billingAddress);

        Parcel parcel = Parcel.obtain();
        expected.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureRequest actual = new ThreeDSecureRequest(parcel);

        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getNonce(), actual.getNonce());
        assertEquals(expected.getMobilePhoneNumber(), actual.getMobilePhoneNumber());
        assertEquals(expected.getEmail(), actual.getEmail());
        assertEquals(expected.getShippingMethod(), actual.getShippingMethod());
        assertEquals(expected.getBillingAddress().getFirstName(), actual.getBillingAddress().getFirstName());
        assertEquals(expected.getBillingAddress().getLastName(), actual.getBillingAddress().getLastName());
        assertEquals(expected.getBillingAddress().getPhoneNumber(), actual.getBillingAddress().getPhoneNumber());
        assertEquals(expected.getBillingAddress().getStreetAddress(), actual.getBillingAddress().getStreetAddress());
        assertEquals(expected.getBillingAddress().getExtendedAddress(), actual.getBillingAddress().getExtendedAddress());
        assertEquals(expected.getBillingAddress().getLocality(), actual.getBillingAddress().getLocality());
        assertEquals(expected.getBillingAddress().getRegion(), actual.getBillingAddress().getRegion());
        assertEquals(expected.getBillingAddress().getCountryCodeAlpha2(), actual.getBillingAddress().getCountryCodeAlpha2());
        assertEquals(expected.getBillingAddress().getPostalCode(), actual.getBillingAddress().getPostalCode());
    }

    @Test
    public void buildsAllParameters() throws JSONException{
        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. 3")
                .locality("Oakland")
                .region("CA")
                .postalCode("94602")
                .countryCodeAlpha2("US")
                .firstName("John")
                .lastName("Fakerson")
                .phoneNumber("5151231234");

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .mobilePhoneNumber("5151234321")
                .email("tester@example.com")
                .shippingMethod("03")
                .billingAddress(billingAddress);

        JSONObject jsonParams = new JSONObject(request.build());
        JSONObject jsonCustomer = jsonParams.getJSONObject("customer");
        JSONObject jsonBillingAddress = jsonCustomer.getJSONObject("billingAddress");

        assertEquals("1.00", jsonParams.get("amount"));
        assertEquals("5151234321", jsonCustomer.get("mobilePhoneNumber"));
        assertEquals("tester@example.com", jsonCustomer.get("email"));
        assertEquals("03", jsonCustomer.get("shippingMethod"));
        assertEquals("123 Fake St.", jsonBillingAddress.get("line1"));
        assertEquals("Apt. 3", jsonBillingAddress.get("line2"));
        assertEquals("Oakland", jsonBillingAddress.get("city"));
        assertEquals("CA", jsonBillingAddress.get("state"));
        assertEquals("94602", jsonBillingAddress.get("postalCode"));
        assertEquals("US", jsonBillingAddress.get("countryCode"));
        assertEquals("John", jsonBillingAddress.get("firstName"));
        assertEquals("Fakerson", jsonBillingAddress.get("lastName"));
        assertEquals("5151231234", jsonBillingAddress.get("phoneNumber"));
    }

    @Test
    public void buildsPartialParameters() throws JSONException{
        ThreeDSecurePostalAddress billingAddress = new ThreeDSecurePostalAddress()
                .streetAddress("123 Fake St.")
                .extendedAddress("Apt. 3")
                .locality("Oakland")
                .region("CA")
                .postalCode("94602")
                .firstName("John")
                .lastName("Fakerson");

        ThreeDSecureRequest request = new ThreeDSecureRequest()
                .nonce("a-nonce")
                .amount("1.00")
                .mobilePhoneNumber("5151234321")
                .shippingMethod("03")
                .billingAddress(billingAddress);

        JSONObject jsonParams = new JSONObject(request.build());
        JSONObject jsonCustomer = jsonParams.getJSONObject("customer");
        JSONObject jsonBillingAddress = jsonCustomer.getJSONObject("billingAddress");

        assertEquals("1.00", jsonParams.get("amount"));
        assertEquals("5151234321", jsonCustomer.get("mobilePhoneNumber"));
        assertTrue(jsonCustomer.isNull("email"));
        assertEquals("03", jsonCustomer.get("shippingMethod"));
        assertEquals("123 Fake St.", jsonBillingAddress.get("line1"));
        assertEquals("Apt. 3", jsonBillingAddress.get("line2"));
        assertEquals("Oakland", jsonBillingAddress.get("city"));
        assertEquals("CA", jsonBillingAddress.get("state"));
        assertEquals("94602", jsonBillingAddress.get("postalCode"));
        assertTrue(jsonBillingAddress.isNull("countryCode"));
        assertEquals("John", jsonBillingAddress.get("firstName"));
        assertEquals("Fakerson", jsonBillingAddress.get("lastName"));
        assertTrue(jsonBillingAddress.isNull("phoneNumber"));
    }
}
