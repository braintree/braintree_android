package com.braintreepayments.api.models;

import android.test.AndroidTestCase;
import com.braintreepayments.testutils.FixturesHelper;

public class GoogleWalletCardTest extends AndroidTestCase {

    public void testCanCreateFromJson() {
        String googleWalletString = FixturesHelper.stringFromFixture(mContext,
                "payment_methods/google_wallet_card.json");

        GoogleWalletCard googleWalletCard = GoogleWalletCard.fromJson(googleWalletString);

        assertEquals("Google Wallet", googleWalletCard.getTypeLabel());
        assertEquals("fake-google-wallet-card-nonce", googleWalletCard.getNonce());
        assertEquals("Google Wallet", googleWalletCard.getDescription());
        assertEquals("11", googleWalletCard.getLastTwo());
    }
}
