package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.api.models.BinDataUnitTest.assertBinData;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutNonceUnitTest {

    @Test
    public void fromJson_createsVisaCheckoutNonce() throws JSONException {
        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJson(
                stringFromFixture("payment_methods/visa_checkout_response.json"));

        assertEquals("11", visaCheckoutNonce.getLastTwo());
        assertEquals("Visa", visaCheckoutNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", visaCheckoutNonce.getNonce());
        assertEquals("ending in ••11", visaCheckoutNonce.getDescription());
        assertFalse(visaCheckoutNonce.isDefault());
        assertEquals("Visa Checkout", visaCheckoutNonce.getTypeLabel());
        assertEquals("callId", visaCheckoutNonce.getCallId());

        assertNotNull(visaCheckoutNonce.getBillingAddress());
        assertEquals("billingFirstName", visaCheckoutNonce.getBillingAddress().getFirstName());
        assertEquals("billingLastName", visaCheckoutNonce.getBillingAddress().getLastName());
        assertEquals("billingStreetAddress", visaCheckoutNonce.getBillingAddress().getStreetAddress());
        assertEquals("billingExtendedAddress", visaCheckoutNonce.getBillingAddress().getExtendedAddress());
        assertEquals("billingLocality", visaCheckoutNonce.getBillingAddress().getLocality());
        assertEquals("billingRegion", visaCheckoutNonce.getBillingAddress().getRegion());
        assertEquals("billingPostalCode", visaCheckoutNonce.getBillingAddress().getPostalCode());
        assertEquals("billingCountryCode", visaCheckoutNonce.getBillingAddress().getCountryCode());

        assertNotNull(visaCheckoutNonce.getShippingAddress());
        assertEquals("shippingFirstName", visaCheckoutNonce.getShippingAddress().getFirstName());
        assertEquals("shippingLastName", visaCheckoutNonce.getShippingAddress().getLastName());
        assertEquals("shippingStreetAddress", visaCheckoutNonce.getShippingAddress().getStreetAddress());
        assertEquals("shippingExtendedAddress", visaCheckoutNonce.getShippingAddress().getExtendedAddress());
        assertEquals("shippingLocality", visaCheckoutNonce.getShippingAddress().getLocality());
        assertEquals("shippingRegion", visaCheckoutNonce.getShippingAddress().getRegion());
        assertEquals("shippingPostalCode", visaCheckoutNonce.getShippingAddress().getPostalCode());
        assertEquals("shippingCountryCode", visaCheckoutNonce.getShippingAddress().getCountryCode());

        assertNotNull(visaCheckoutNonce.getUserData());
        assertEquals("userFirstName", visaCheckoutNonce.getUserData().getUserFirstName());
        assertEquals("userLastName", visaCheckoutNonce.getUserData().getUserLastName());
        assertEquals("userFullName", visaCheckoutNonce.getUserData().getUserFullName());
        assertEquals("userUserName", visaCheckoutNonce.getUserData().getUsername());
        assertEquals("userEmail", visaCheckoutNonce.getUserData().getUserEmail());

        assertNotNull(visaCheckoutNonce.getBinData());
        assertEquals(UNKNOWN, visaCheckoutNonce.getBinData().getPrepaid());
        assertEquals(YES, visaCheckoutNonce.getBinData().getHealthcare());
        assertEquals(NO, visaCheckoutNonce.getBinData().getDebit());
        assertEquals(UNKNOWN, visaCheckoutNonce.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, visaCheckoutNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, visaCheckoutNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, visaCheckoutNonce.getBinData().getIssuingBank());
        assertEquals("Something", visaCheckoutNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", visaCheckoutNonce.getBinData().getProductId());
    }

    @Test
    public void fromJson_whenNoCallId_createsVisaCheckoutNonceWithEmptyCallId() throws JSONException {
        JSONObject visaCheckoutResponseJson = new JSONObject(stringFromFixture(
                "payment_methods/visa_checkout_response.json"));

        JSONArray visaCheckoutCardsJson = visaCheckoutResponseJson.getJSONArray("visaCheckoutCards");
        JSONObject visaCheckoutNonceJson = visaCheckoutCardsJson.getJSONObject(0);
        visaCheckoutNonceJson.remove("callId");

        visaCheckoutCardsJson.put(0, visaCheckoutNonceJson);
        visaCheckoutResponseJson.put("visaCheckoutCards", visaCheckoutCardsJson);

        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJson(visaCheckoutResponseJson.toString());

        assertEquals("", visaCheckoutNonce.getCallId());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJson(
                stringFromFixture("payment_methods/visa_checkout_response.json"));

        Parcel parcel = Parcel.obtain();
        visaCheckoutNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutNonce actual = VisaCheckoutNonce.CREATOR.createFromParcel(parcel);

        assertEquals(visaCheckoutNonce.getNonce(), actual.getNonce());
        assertEquals(visaCheckoutNonce.getDescription(), actual.getDescription());
        assertEquals(visaCheckoutNonce.isDefault(), actual.isDefault());

        assertEquals(visaCheckoutNonce.getLastTwo(), actual.getLastTwo());
        assertEquals(visaCheckoutNonce.getCardType(), actual.getCardType());

        assertBinData(visaCheckoutNonce.getBinData(), actual.getBinData());
        assertVisaCheckoutAddress(visaCheckoutNonce.getBillingAddress(), actual.getBillingAddress());
        assertVisaCheckoutAddress(visaCheckoutNonce.getShippingAddress(), actual.getShippingAddress());

        assertEquals(visaCheckoutNonce.getCallId(), actual.getCallId());

        VisaCheckoutUserData expectedUserData = visaCheckoutNonce.getUserData();
        VisaCheckoutUserData actualUserData = visaCheckoutNonce.getUserData();

        assertEquals(expectedUserData.getUserFirstName(), actualUserData.getUserFirstName());
        assertEquals(expectedUserData.getUserLastName(), actualUserData.getUserLastName());
        assertEquals(expectedUserData.getUserFullName(), actualUserData.getUserFullName());
        assertEquals(expectedUserData.getUserEmail(), actualUserData.getUserEmail());
    }

    private void assertVisaCheckoutAddress(VisaCheckoutAddress expected, VisaCheckoutAddress actual) {
        assertEquals(expected.getFirstName(), actual.getFirstName());
        assertEquals(expected.getLastName(), actual.getLastName());
        assertEquals(expected.getStreetAddress(), actual.getStreetAddress());
        assertEquals(expected.getExtendedAddress(), actual.getExtendedAddress());
        assertEquals(expected.getLocality(), actual.getLocality());
        assertEquals(expected.getRegion(), actual.getRegion());
        assertEquals(expected.getPostalCode(), actual.getPostalCode());
        assertEquals(expected.getCountryCode(), actual.getCountryCode());
        assertEquals(expected.getPhoneNumber(), actual.getPhoneNumber());
    }
}
