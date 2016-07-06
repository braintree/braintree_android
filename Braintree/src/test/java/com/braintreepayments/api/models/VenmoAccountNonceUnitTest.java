package com.braintreepayments.api.models;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class VenmoAccountNonceUnitTest {

    private static final String NONCE = "nonce";
    private static final String DESCRIPTION = "description";
    private static final String USERNAME = "username";
    private static final VenmoAccountNonce VENMO_NONCE = new VenmoAccountNonce(NONCE, DESCRIPTION, USERNAME);

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
