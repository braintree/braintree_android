package com.braintreepayments.api.visacheckout;

import static com.braintreepayments.api.testutils.Assertions.assertBinDataEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

import android.os.Parcel;

import com.braintreepayments.api.card.BinData;
import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VisaCheckoutNonceUnitTest {

    @Test
    public void fromJson_createsVisaCheckoutNonce() throws JSONException {
        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJSON(new JSONObject(
                Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE));

        assertEquals("11", visaCheckoutNonce.getLastTwo());
        assertEquals("Visa", visaCheckoutNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", visaCheckoutNonce.getString());
        assertFalse(visaCheckoutNonce.isDefault());
        assertEquals("callId", visaCheckoutNonce.getCallId());

        assertNotNull(visaCheckoutNonce.getBillingAddress());
        assertEquals("billingFirstName", visaCheckoutNonce.getBillingAddress().getFirstName());
        assertEquals("billingLastName", visaCheckoutNonce.getBillingAddress().getLastName());
        assertEquals("billingStreetAddress",
                visaCheckoutNonce.getBillingAddress().getStreetAddress());
        assertEquals("billingExtendedAddress",
                visaCheckoutNonce.getBillingAddress().getExtendedAddress());
        assertEquals("billingLocality", visaCheckoutNonce.getBillingAddress().getLocality());
        assertEquals("billingRegion", visaCheckoutNonce.getBillingAddress().getRegion());
        assertEquals("billingPostalCode", visaCheckoutNonce.getBillingAddress().getPostalCode());
        assertEquals("billingCountryCode", visaCheckoutNonce.getBillingAddress().getCountryCode());

        assertNotNull(visaCheckoutNonce.getShippingAddress());
        assertEquals("shippingFirstName", visaCheckoutNonce.getShippingAddress().getFirstName());
        assertEquals("shippingLastName", visaCheckoutNonce.getShippingAddress().getLastName());
        assertEquals("shippingStreetAddress",
                visaCheckoutNonce.getShippingAddress().getStreetAddress());
        assertEquals("shippingExtendedAddress",
                visaCheckoutNonce.getShippingAddress().getExtendedAddress());
        assertEquals("shippingLocality", visaCheckoutNonce.getShippingAddress().getLocality());
        assertEquals("shippingRegion", visaCheckoutNonce.getShippingAddress().getRegion());
        assertEquals("shippingPostalCode", visaCheckoutNonce.getShippingAddress().getPostalCode());
        assertEquals("shippingCountryCode",
                visaCheckoutNonce.getShippingAddress().getCountryCode());

        assertNotNull(visaCheckoutNonce.getUserData());
        assertEquals("userFirstName", visaCheckoutNonce.getUserData().getUserFirstName());
        assertEquals("userLastName", visaCheckoutNonce.getUserData().getUserLastName());
        assertEquals("userFullName", visaCheckoutNonce.getUserData().getUserFullName());
        assertEquals("userUserName", visaCheckoutNonce.getUserData().getUsername());
        assertEquals("userEmail", visaCheckoutNonce.getUserData().getUserEmail());

        assertNotNull(visaCheckoutNonce.getBinData());
        assertEquals(BinData.BinType.Unknown, visaCheckoutNonce.getBinData().getPrepaid());
        assertEquals(BinData.BinType.Yes, visaCheckoutNonce.getBinData().getHealthcare());
        assertEquals(BinData.BinType.No, visaCheckoutNonce.getBinData().getDebit());
        assertEquals(BinData.BinType.Unknown, visaCheckoutNonce.getBinData().getDurbinRegulated());
        assertEquals(BinData.BinType.Unknown, visaCheckoutNonce.getBinData().getCommercial());
        assertEquals(BinData.BinType.Unknown, visaCheckoutNonce.getBinData().getPayroll());
        assertEquals(BinData.BinType.Unknown.name(), visaCheckoutNonce.getBinData().getIssuingBank());
        assertEquals("Something", visaCheckoutNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", visaCheckoutNonce.getBinData().getProductId());
    }

    @Test
    public void fromJson_whenNoCallId_createsVisaCheckoutNonceWithEmptyCallId()
            throws JSONException {
        JSONObject visaCheckoutResponseJson =
                new JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE);

        JSONArray visaCheckoutCardsJson =
                visaCheckoutResponseJson.getJSONArray("visaCheckoutCards");
        JSONObject visaCheckoutNonceJson = visaCheckoutCardsJson.getJSONObject(0);
        visaCheckoutNonceJson.remove("callId");

        visaCheckoutCardsJson.put(0, visaCheckoutNonceJson);
        visaCheckoutResponseJson.put("visaCheckoutCards", visaCheckoutCardsJson);

        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJSON(visaCheckoutResponseJson);

        assertEquals("", visaCheckoutNonce.getCallId());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        VisaCheckoutNonce visaCheckoutNonce = VisaCheckoutNonce.fromJSON(
                new JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE));

        Parcel parcel = Parcel.obtain();
        visaCheckoutNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        VisaCheckoutNonce actual = VisaCheckoutNonce.CREATOR.createFromParcel(parcel);

        assertEquals(visaCheckoutNonce.getString(), actual.getString());
        assertEquals(visaCheckoutNonce.isDefault(), actual.isDefault());
        assertEquals(visaCheckoutNonce.getLastTwo(), actual.getLastTwo());
        assertEquals(visaCheckoutNonce.getCardType(), actual.getCardType());
        assertBinDataEqual(visaCheckoutNonce.getBinData(), actual.getBinData());
        assertVisaCheckoutAddress(visaCheckoutNonce.getBillingAddress(),
                actual.getBillingAddress());
        assertVisaCheckoutAddress(visaCheckoutNonce.getShippingAddress(),
                actual.getShippingAddress());
        assertEquals(visaCheckoutNonce.getCallId(), actual.getCallId());
        assertEquals(visaCheckoutNonce.getUserData().getUserFirstName(),
                actual.getUserData().getUserFirstName());
        assertEquals(visaCheckoutNonce.getUserData().getUserLastName(),
                actual.getUserData().getUserLastName());
        assertEquals(visaCheckoutNonce.getUserData().getUserFullName(),
                actual.getUserData().getUserFullName());
        assertEquals(visaCheckoutNonce.getUserData().getUserEmail(),
                actual.getUserData().getUserEmail());
    }

    private void assertVisaCheckoutAddress(VisaCheckoutAddress expected,
                                           VisaCheckoutAddress actual) {
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
