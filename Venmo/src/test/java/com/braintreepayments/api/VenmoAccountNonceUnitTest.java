package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class VenmoAccountNonceUnitTest {

    private static final String NONCE = "nonce";
    private static final String USERNAME = "username";
    private static final VenmoAccountNonce VENMO_NONCE = new VenmoAccountNonce(NONCE, USERNAME, false);

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
            VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE));

        assertEquals("venmojoe", venmoAccountNonce.getUsername());
        assertEquals("fake-venmo-nonce", venmoAccountNonce.getString());
    }

    @Test
    public void fromJson_withPaymentMethodId_parsesResponse() throws JSONException {
        VenmoAccountNonce venmoAccountNonce =
                VenmoAccountNonce.fromJSON(new JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_JSON));

        assertEquals("@sampleuser", venmoAccountNonce.getUsername());
        assertEquals("sample-payment-method-id", venmoAccountNonce.getString());
        assertEquals(PaymentMethodType.VENMO, venmoAccountNonce.getType());
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
    public void parcelsCorrectly() {
        Parcel parcel = Parcel.obtain();
        VENMO_NONCE.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        VenmoAccountNonce parceled = VenmoAccountNonce.CREATOR.createFromParcel(parcel);

        assertEquals(NONCE, parceled.getString());
        assertEquals(USERNAME, parceled.getUsername());
    }
}
