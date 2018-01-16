package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.models.BinData.NO;
import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.api.models.BinData.YES;
import static com.braintreepayments.testutils.Assertions.assertBinDataEqual;
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
        assertEquals("1111", cardNonce.getLastFour());
        assertNotNull(cardNonce.getThreeDSecureInfo());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNotNull(cardNonce.getBinData());
        assertEquals(UNKNOWN, cardNonce.getBinData().getPrepaid());
        assertEquals(YES, cardNonce.getBinData().getHealthcare());
        assertEquals(NO, cardNonce.getBinData().getDebit());
        assertEquals(UNKNOWN, cardNonce.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, cardNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, cardNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, cardNonce.getBinData().getIssuingBank());
        assertEquals("Something", cardNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", cardNonce.getBinData().getProductId());
    }

    @Test
    public void canCreateCardFromTokenizeCreditCardGraphQLResponse() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJson(stringFromFixture("response/graphql/credit_card.json"));

        assertEquals("Visa", cardNonce.getTypeLabel());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.getNonce());
        assertEquals("ending in ••11", cardNonce.getDescription());
        assertEquals("11", cardNonce.getLastTwo());
        assertEquals("1111", cardNonce.getLastFour());
        assertNotNull(cardNonce.getThreeDSecureInfo());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNotNull(cardNonce.getBinData());
        assertEquals(YES, cardNonce.getBinData().getPrepaid());
        assertEquals(UNKNOWN, cardNonce.getBinData().getHealthcare());
        assertEquals(NO, cardNonce.getBinData().getDebit());
        assertEquals(YES, cardNonce.getBinData().getDurbinRegulated());
        assertEquals(NO, cardNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, cardNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, cardNonce.getBinData().getIssuingBank());
        assertEquals("USA", cardNonce.getBinData().getCountryOfIssuance());
        assertEquals(UNKNOWN, cardNonce.getBinData().getProductId());
    }

    @Test
    public void handlesGraphQLUnknownCardResponses() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJson(stringFromFixture("response/graphql/unknown_credit_card.json"));

        assertEquals("Unknown", cardNonce.getTypeLabel());
        assertEquals("Unknown", cardNonce.getCardType());
        assertEquals("tokencc_3bbd22_fpjshh_bqbvh5_mkf3nf_smz", cardNonce.getNonce());
        assertEquals("", cardNonce.getDescription());
        assertEquals("", cardNonce.getLastTwo());
        assertEquals("", cardNonce.getLastFour());
        assertNotNull(cardNonce.getThreeDSecureInfo());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNotNull(cardNonce.getBinData());
        assertEquals(UNKNOWN, cardNonce.getBinData().getPrepaid());
        assertEquals(UNKNOWN, cardNonce.getBinData().getHealthcare());
        assertEquals(UNKNOWN, cardNonce.getBinData().getDebit());
        assertEquals(UNKNOWN, cardNonce.getBinData().getDurbinRegulated());
        assertEquals(UNKNOWN, cardNonce.getBinData().getCommercial());
        assertEquals(UNKNOWN, cardNonce.getBinData().getPayroll());
        assertEquals(UNKNOWN, cardNonce.getBinData().getIssuingBank());
        assertEquals(UNKNOWN, cardNonce.getBinData().getCountryOfIssuance());
        assertEquals(UNKNOWN, cardNonce.getBinData().getProductId());
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
        assertEquals("1111", parceled.getLastFour());
        assertFalse(parceled.isDefault());
        assertBinDataEqual(cardNonce.getBinData(), parceled.getBinData());
    }
}