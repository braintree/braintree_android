package com.braintreepayments.api.models;

import android.test.InstrumentationTestCase;

import com.braintreepayments.api.FixturesHelper;

public class CardTest extends InstrumentationTestCase {

    public void testCanCreateCardFromJson() {
        String cardString = FixturesHelper.stringFromFixture(getInstrumentation().getContext(),
                "payment_methods/visa_credit_card.json");

        Card card = Card.fromJson(cardString);

        assertEquals("Visa", card.getTypeLabel());
        assertEquals("123456-12345-12345-a-adfa", card.getNonce());
        assertEquals("ending in ••11", card.getDescription());
        assertEquals("11", card.getLastTwo());
    }
}