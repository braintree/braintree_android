package com.braintreepayments.api.models;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class VenmoAccountNonceTest {

    private static final String NONCE = "nonce";
    private static final String DESCRIPTION = "description";
    private static final String USERNAME = "username";
    private static final VenmoAccountNonce VENMO_NONCE = new VenmoAccountNonce(NONCE, DESCRIPTION, USERNAME);

    @Test(timeout = 1000)
    public void getTypeLabel_returnsPayWithVenmo() {
        assertEquals("Venmo", VENMO_NONCE.getTypeLabel());
    }

    @Test(timeout = 1000)
    public void getNonce_returnsNonce() {
        assertEquals(NONCE, VENMO_NONCE.getNonce());
    }

    @Test(timeout = 1000)
    public void getDescription_returnsDescription() {
        assertEquals(DESCRIPTION, VENMO_NONCE.getDescription());
    }

    @Test(timeout = 1000)
    public void getUsername_returnsUsername() {
        assertEquals(USERNAME, VENMO_NONCE.getUsername());
    }

    @Test(timeout = 1000)
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
