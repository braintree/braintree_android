package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class CardNonceTest {

    @Test(timeout = 1000)
    @SmallTest
    public void canCreateCardFromJson() throws JSONException {
        String cardString = stringFromFixture("payment_methods/visa_credit_card_response.json");

        CardNonce cardNonce = CardNonce.fromJson(cardString);

        assertEquals("Visa", cardNonce.getTypeLabel());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getNonce());
        assertEquals("ending in ••11", cardNonce.getDescription());
        assertEquals("11", cardNonce.getLastTwo());
    }
}