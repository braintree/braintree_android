package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class AndroidPayCardNonceTest {

    @Test
    public void testCanCreateFromJson() throws JSONException {
        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromJson(
                stringFromFixture("payment_methods/android_pay_card_response.json"));

        assertEquals("Android Pay", androidPayCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCardNonce.getNonce());
        assertEquals("Android Pay", androidPayCardNonce.getDescription());
        assertEquals("Visa", androidPayCardNonce.getCardType());
        assertEquals("11", androidPayCardNonce.getLastTwo());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromJson(
                stringFromFixture("payment_methods/android_pay_card_response.json"));
        Parcel parcel = Parcel.obtain();
        androidPayCardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        AndroidPayCardNonce parceled = AndroidPayCardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Android Pay", parceled.getTypeLabel());
        assertEquals("fake-android-pay-nonce", parceled.getNonce());
        assertEquals("Android Pay", parceled.getDescription());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("11", parceled.getLastTwo());
    }
}
