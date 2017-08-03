package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class CardNonceUnitTest {

    @Test
    public void canCreateCardFromJson() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJson(stringFromFixture("payment_methods/visa_credit_card_response.json"));

        assertEquals("Visa", cardNonce.getTypeLabel());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getNonce());
        assertEquals("ending in ••11", cardNonce.getDescription());
        assertEquals("11", cardNonce.getLastTwo());
    }

    @Test
    public void canCreateCardFromJsonWithBinData() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJson(stringFromFixture("payment_methods/visa_credit_card_response_with_bin_data.json"));

        assertEquals("Visa", cardNonce.getTypeLabel());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getNonce());
        assertEquals("ending in ••11", cardNonce.getDescription());
        assertEquals("11", cardNonce.getLastTwo());
        assertNotNull(cardNonce.getBinData());
        BinData binData = cardNonce.getBinData();
        assertEquals(UNKNOWN, binData.getPrepaid());
        assertEquals(YES, binData.getHealthcare());
        assertEquals(NO, binData.getDebit());
        assertEquals(UNKNOWN, binData.getDurbinRegulated());
        assertEquals(UNKNOWN, binData.getCommercial());
        assertEquals(UNKNOWN, binData.getPayroll());
        assertEquals(UNKNOWN, binData.getIssuingBank());
        assertEquals("Something", binData.getCountryOfIssuance());
        assertEquals("123", binData.getProductId());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJson(stringFromFixture("payment_methods/visa_credit_card_response.json"));
        Parcel parcel = Parcel.obtain();
        cardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        CardNonce parceled = CardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Visa", parceled.getTypeLabel());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("123456-12345-12345-a-adfa", parceled.getNonce());
        assertEquals("ending in ••11", parceled.getDescription());
        assertEquals("11", parceled.getLastTwo());
        assertFalse(parceled.isDefault());
    }
}