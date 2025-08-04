package com.braintreepayments.api.localpayment;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import com.braintreepayments.api.testutils.Fixtures;

@RunWith(RobolectricTestRunner.class)
public class LocalPaymentNonceUnitTest {

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        LocalPaymentNonce result = LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));

        assertNotNull(result);
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", result.getString());
        assertEquals("jon@getbraintree.com", result.getEmail());
        assertEquals("836486 of 22321 Park Lake", result.getShippingAddress().getStreetAddress());
        assertEquals("Apt B", result.getShippingAddress().getExtendedAddress());
        assertEquals("Den Haag", result.getShippingAddress().getLocality());
        assertEquals("CA", result.getShippingAddress().getRegion());
        assertEquals("2585 GJ", result.getShippingAddress().getPostalCode());
        assertEquals("NL", result.getShippingAddress().getCountryCodeAlpha2());
        assertEquals("Jon Doe", result.getShippingAddress().getRecipientName());
        assertEquals("Jon", result.getGivenName());
        assertEquals("Doe", result.getSurname());
        assertEquals("9KQSUZTL7YZQ4", result.getPayerId());
        assertEquals("084afbf1db15445587d30bc120a23b09", result.getClientMetadataId());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        LocalPaymentNonce result = LocalPaymentNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE));
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        LocalPaymentNonce parceled = LocalPaymentNonce.CREATOR.createFromParcel(parcel);

        assertNotNull(parceled);
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", parceled.getString());
        assertEquals("jon@getbraintree.com", parceled.getEmail());
        assertEquals("836486 of 22321 Park Lake", parceled.getShippingAddress().getStreetAddress());
        assertEquals("Apt B", parceled.getShippingAddress().getExtendedAddress());
        assertEquals("Den Haag", parceled.getShippingAddress().getLocality());
        assertEquals("CA", parceled.getShippingAddress().getRegion());
        assertEquals("2585 GJ", parceled.getShippingAddress().getPostalCode());
        assertEquals("NL", parceled.getShippingAddress().getCountryCodeAlpha2());
        assertEquals("Jon Doe", parceled.getShippingAddress().getRecipientName());
        assertEquals("Jon", parceled.getGivenName());
        assertEquals("Doe", parceled.getSurname());
        assertEquals("9KQSUZTL7YZQ4", parceled.getPayerId());
        assertEquals("084afbf1db15445587d30bc120a23b09", parceled.getClientMetadataId());
    }

    @Test
    public void when_fromJSON_is_called_missing_fields_default_to_an_empty_string() throws JSONException {
        LocalPaymentNonce result = LocalPaymentNonce.fromJSON(
            new JSONObject(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_MISSING_FIELDS_RESPONSE)
        );

        assertNotNull(result);
        assertEquals("141b7583-2922-1ce6-1f2e-f352b69115d6", result.getString());
        assertNull(result.getEmail());
        assertNull(result.getShippingAddress().getStreetAddress());
        assertNull(result.getShippingAddress().getExtendedAddress());
        assertNull(result.getShippingAddress().getLocality());
        assertNull(result.getShippingAddress().getRegion());
        assertNull(result.getShippingAddress().getPostalCode());
        assertNull(result.getShippingAddress().getCountryCodeAlpha2());
        assertNull(result.getShippingAddress().getRecipientName());
        assertEquals("", result.getGivenName());
        assertEquals("", result.getSurname());
        assertEquals("", result.getPayerId());
        assertEquals("c7ce54e0cde5406785b13c99086a9f4c", result.getClientMetadataId());
    }
}
