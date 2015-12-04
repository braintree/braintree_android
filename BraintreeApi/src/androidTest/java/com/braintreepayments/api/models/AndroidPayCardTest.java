package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;

public class AndroidPayCardTest extends AndroidTestCase {

    public void testCanCreateFromJson() {
        String androidPayString = stringFromFixture(mContext, "payment_methods/android_pay_card.json");

        AndroidPayCard androidPayCard = AndroidPayCard.fromJson(androidPayString);

        assertEquals("Android Pay", androidPayCard.getTypeLabel());
        assertEquals("fake-android-pay-nonce", androidPayCard.getNonce());
        assertEquals("Android Pay", androidPayCard.getDescription());
        assertEquals("11", androidPayCard.getLastTwo());
    }
}
