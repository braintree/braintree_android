package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class LocalPaymentResultUnitTest {

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        LocalPaymentResult result = LocalPaymentResult.fromJson(
                stringFromFixture("payment_methods/local_payment_response.json"));

        assertNotNull(result);
        assertEquals("PayPal", result.getDescription());
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", result.getNonce());
        assertEquals("jon@getbraintree.com", result.getEmail());
        assertEquals("PayPalAccount", result.getTypeLabel());
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
        LocalPaymentResult result = LocalPaymentResult.fromJson(
                stringFromFixture("payment_methods/local_payment_response.json"));
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        LocalPaymentResult parceled = LocalPaymentResult.CREATOR.createFromParcel(parcel);

        assertNotNull(parceled);
        assertEquals("PayPal", parceled.getDescription());
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", parceled.getNonce());
        assertEquals("jon@getbraintree.com", parceled.getEmail());
        assertEquals("PayPalAccount", parceled.getTypeLabel());
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
}
