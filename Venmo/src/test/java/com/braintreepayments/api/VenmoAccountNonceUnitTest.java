package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountNonceUnitTest {

    private static final String NONCE = "venmo-nonce";
    private static final String USERNAME = "venmo-username";
    private static final VenmoAccountNonce VENMO_NONCE = new VenmoAccountNonce(NONCE, USERNAME, false);

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
            VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        assertEquals("venmojoe", venmoAccountNonce.getUsername());
        assertEquals("fake-venmo-nonce", venmoAccountNonce.getString());
        assertTrue(venmoAccountNonce.isDefault());
    }

    @Test
    public void fromJson_withPaymentMethodId_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
                VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON));

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
                VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON));

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
