package com.braintreepayments.api.venmo;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import com.braintreepayments.api.testutils.Fixtures;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountNonceUnitTest {

    private static final String NONCE = "venmo-nonce";
    private static final String USERNAME = "venmo-username";
    private static final VenmoAccountNonce VENMO_NONCE = new VenmoAccountNonce(
        NONCE,
        false,
        null,
        null,
        null,
        null,
        null,
        USERNAME,
        null,
        null
    );

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
                VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        assertEquals("venmojoe", venmoAccountNonce.getUsername());
        assertEquals("fake-venmo-nonce", venmoAccountNonce.getString());
        assertTrue(venmoAccountNonce.isDefault());
    }

    @Test
    public void fromJson_withPaymentMethodId_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
                VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON));

        assertEquals("@sampleuser", venmoAccountNonce.getUsername());
        assertEquals("sample-payment-method-id", venmoAccountNonce.getString());
        assertEquals("venmo-email", venmoAccountNonce.getEmail());
        assertEquals("venmo-external-id", venmoAccountNonce.getExternalId());
        assertEquals("venmo-first-name", venmoAccountNonce.getFirstName());
        assertEquals("venmo-last-name", venmoAccountNonce.getLastName());
        assertEquals("venmo-phone-number", venmoAccountNonce.getPhoneNumber());
        assertFalse(venmoAccountNonce.isDefault());
    }

    @Test
    public void fromJson_withShippingAndBillingAddresses_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
                VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON_WITH_ADDRESSES));

        assertEquals("123 Fake St.", venmoAccountNonce.getBillingAddress().getStreetAddress());
        assertEquals("Apt. 3", venmoAccountNonce.getBillingAddress().getExtendedAddress());
        assertEquals("Oakland", venmoAccountNonce.getBillingAddress().getLocality());
        assertEquals("CA", venmoAccountNonce.getBillingAddress().getRegion());
        assertEquals("94602", venmoAccountNonce.getBillingAddress().getPostalCode());
        assertEquals("US", venmoAccountNonce.getBillingAddress().getCountryCodeAlpha2());

        assertEquals("789 Fake St.", venmoAccountNonce.getShippingAddress().getStreetAddress());
        assertEquals("Apt. 2", venmoAccountNonce.getShippingAddress().getExtendedAddress());
        assertEquals("Dallas", venmoAccountNonce.getShippingAddress().getLocality());
        assertEquals("TX", venmoAccountNonce.getShippingAddress().getRegion());
        assertEquals("75001", venmoAccountNonce.getShippingAddress().getPostalCode());
        assertEquals("US", venmoAccountNonce.getShippingAddress().getCountryCodeAlpha2());
    }

    @Test
    public void fromJson_withPaymentMethodIdAndNullPayerInfo_parsesResponse() throws JSONException {
        JSONObject json =
                new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_WITH_NULL_PAYER_INFO_JSON);
        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.fromJSON(json);

        assertEquals("@sampleuser", venmoAccountNonce.getUsername());
        assertEquals("sample-payment-method-id", venmoAccountNonce.getString());
        assertNull(venmoAccountNonce.getEmail());
        assertNull(venmoAccountNonce.getExternalId());
        assertNull(venmoAccountNonce.getFirstName());
        assertNull(venmoAccountNonce.getLastName());
        assertNull(venmoAccountNonce.getPhoneNumber());
        assertFalse(venmoAccountNonce.isDefault());
    }

    @Test
    public void getNonce_returnsNonce() {
        assertEquals(NONCE, VENMO_NONCE.getString());
    }

    @Test
    public void getUsername_returnsUsername() {
        assertEquals(USERNAME, VENMO_NONCE.getUsername());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
                VenmoAccountNonce.fromJSON(
                        new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON));

        Parcel parcel = Parcel.obtain();
        venmoAccountNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        VenmoAccountNonce parceled = VenmoAccountNonce.CREATOR.createFromParcel(parcel);

        assertEquals("@sampleuser", parceled.getUsername());
        assertEquals("sample-payment-method-id", parceled.getString());
        assertEquals("venmo-email", parceled.getEmail());
        assertEquals("venmo-external-id", parceled.getExternalId());
        assertEquals("venmo-first-name", parceled.getFirstName());
        assertEquals("venmo-last-name", parceled.getLastName());
        assertEquals("venmo-phone-number", parceled.getPhoneNumber());
    }
}
