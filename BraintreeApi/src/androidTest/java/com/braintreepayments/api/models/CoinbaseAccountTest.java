package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;

public class CoinbaseAccountTest extends AndroidTestCase {

    public void testCoinbaseAccountTypeIsCoinbase() {
        assertEquals("Coinbase", new CoinbaseAccount().getTypeLabel());
    }

    public void testCoinbaseAccountParsesFromJson() {
        String coinbaseAccountString = stringFromFixture(mContext, "coinbase_account.json");
        CoinbaseAccount coinbaseAccount = CoinbaseAccount.fromJson(coinbaseAccountString);

        assertEquals("coinbase-nonce", coinbaseAccount.getNonce());
        assertEquals("coinbase@coinbase.com", coinbaseAccount.getEmail());
    }

    public void testGetEmailReturnsEmptyStringIfDetailsAreNull() {
        CoinbaseAccount coinbaseAccount = new CoinbaseAccount();

        assertEquals("", coinbaseAccount.getEmail());
    }
}
