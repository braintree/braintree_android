package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountNonceUnitTest {

    private static final String NONCE = "nonce";
    private static final String DESCRIPTION = "description";
    private static final String USERNAME = "username";
    private static final VenmoAccountNonce VENMO_NONCE = new VenmoAccountNonce(NONCE, DESCRIPTION, USERNAME);

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce = VenmoAccountNonce.
                fromJson(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE);

        assertEquals("venmojoe", venmoAccountNonce.getDescription());
        assertEquals("venmojoe", venmoAccountNonce.getUsername());
        assertEquals("fake-venmo-nonce", venmoAccountNonce.getNonce());
        assertEquals("Venmo", venmoAccountNonce.getTypeLabel());
    }

    @Test
    public void getTypeLabel_returnsPayWithVenmo() {
        assertEquals("Venmo", VENMO_NONCE.getTypeLabel());
    }

    @Test
    public void getNonce_returnsNonce() {
        assertEquals(NONCE, VENMO_NONCE.getNonce());
    }

    @Test
    public void getDescription_returnsDescription() {
        assertEquals(DESCRIPTION, VENMO_NONCE.getDescription());
    }

    @Test
    public void getUsername_returnsUsername() {
        assertEquals(USERNAME, VENMO_NONCE.getUsername());
    }

    @Test
    public void parcelsCorrectly() {
        Parcel parcel = Parcel.obtain();
        VENMO_NONCE.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        VenmoAccountNonce parceled = VenmoAccountNonce.CREATOR.createFromParcel(parcel);

        assertEquals(NONCE, parceled.getNonce());
        assertEquals(DESCRIPTION, parceled.getDescription());
        assertEquals(USERNAME, parceled.getUsername());
    }
}
