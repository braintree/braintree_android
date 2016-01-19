package com.braintreepayments.api.models;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AndroidPayCardNonceTest {

    @Test(timeout = 1000)
    public void testCanCreateFromJson() throws JSONException {
        String androidPayString = stringFromFixture("payment_methods/android_pay_card_response.json");

        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromJson(androidPayString);

        assertEquals("Android Pay", androidPayCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCardNonce.getNonce());
        assertEquals("Android Pay", androidPayCardNonce.getDescription());
        assertEquals("Visa", androidPayCardNonce.getCardType());
        assertEquals("11", androidPayCardNonce.getLastTwo());
    }

    @Test(timeout = 1000)
    public void parcelsCorrectly() throws JSONException {
        String androidPayString = stringFromFixture("payment_methods/android_pay_card_response.json");
        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromJson(androidPayString);
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
