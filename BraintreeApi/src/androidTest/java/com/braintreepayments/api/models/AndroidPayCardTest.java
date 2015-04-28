package com.braintreepayments.api.models;

import android.test.AndroidTestCase;
import com.braintreepayments.testutils.FixturesHelper;

public class AndroidPayCardTest extends AndroidTestCase {

    public void testCanCreateFromJson() {
        String googleWalletString = FixturesHelper.stringFromFixture(mContext,
                "payment_methods/google_wallet_card.json");

        AndroidPayCard androidPayCard = AndroidPayCard.fromJson(googleWalletString);

        assertEquals("Google Wallet", androidPayCard.getTypeLabel());
        assertEquals("fake-google-wallet-card-nonce", androidPayCard.getNonce());
        assertEquals("Google Wallet", androidPayCard.getDescription());
        assertEquals("11", androidPayCard.getLastTwo());
    }
}
