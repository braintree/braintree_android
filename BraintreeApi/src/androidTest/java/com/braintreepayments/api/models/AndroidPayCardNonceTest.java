package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AndroidPayCardNonceTest {

    @Test(timeout = 1000)
    @SmallTest
    public void testCanCreateFromJson() throws JSONException {
        String androidPayString = stringFromFixture("payment_methods/android_pay_card_response.json");

        AndroidPayCardNonce androidPayCardNonce = AndroidPayCardNonce.fromJson(androidPayString);

        assertEquals("Android Pay", androidPayCardNonce.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCardNonce.getNonce());
        assertEquals("Android Pay", androidPayCardNonce.getDescription());
        assertEquals("Visa", androidPayCardNonce.getCardType());
        assertEquals("11", androidPayCardNonce.getLastTwo());
    }
}
