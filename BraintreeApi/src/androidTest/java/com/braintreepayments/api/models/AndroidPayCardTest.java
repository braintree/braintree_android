package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AndroidPayCardTest {

    @Test(timeout = 1000)
    @SmallTest
    public void testCanCreateFromJson() throws JSONException {
        String androidPayString = stringFromFixture("payment_methods/android_pay_card_response.json");

        AndroidPayCard androidPayCard = AndroidPayCard.fromJson(androidPayString);

        assertEquals("Google Wallet", androidPayCard.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCard.getNonce());
        assertEquals("Android Pay", androidPayCard.getDescription());
        assertEquals("11", androidPayCard.getLastTwo());
    }
}
