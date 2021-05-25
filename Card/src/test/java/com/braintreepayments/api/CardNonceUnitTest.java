package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.BinData.NO;
import static com.braintreepayments.api.BinData.UNKNOWN;
import static com.braintreepayments.api.BinData.YES;
import static com.braintreepayments.api.Assertions.assertBinDataEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class CardNonceUnitTest {

    @Test
    public void fromJSON_withPlainJSONCardNonce_parsesCardNonce() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHOD_CARD));

        assertEquals(PaymentMethodType.CARD, cardNonce.getType());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getString());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("11", cardNonce.getLastTwo());
        assertEquals("1111", cardNonce.getLastFour());
        assertEquals("Visa", cardNonce.getTypeLabel());
        assertEquals("1111", cardNonce.getDescription());
        assertTrue(cardNonce.isDefault());
    }

    @Test
    public void fromJSON_withRESTfulTokenizationResponse_parsesCardNonce() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD));

        assertEquals(PaymentMethodType.CARD, cardNonce.getType());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getString());
        assertEquals("11", cardNonce.getLastTwo());
        assertEquals("1111", cardNonce.getLastFour());
        assertEquals("Visa", cardNonce.getTypeLabel());
        assertEquals("1111", cardNonce.getDescription());
        assertNotNull(cardNonce.getThreeDSecureInfo());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
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
        assertEquals("unregulated",
                cardNonce.getAuthenticationInsight().getRegulationEnvironment());
        assertEquals("01", cardNonce.getExpirationMonth());
        assertEquals("2020", cardNonce.getExpirationYear());
        assertEquals("Joe Smith", cardNonce.getCardholderName());
    }

    @Test
    public void fromJSON_withGraphQLTokenizationResponse_parsesCardNonce() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD));

        assertEquals(PaymentMethodType.CARD, cardNonce.getType());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.getString());
        assertEquals("11", cardNonce.getLastTwo());
        assertEquals("1111", cardNonce.getLastFour());
        assertEquals("Visa", cardNonce.getTypeLabel());
        assertEquals("1111", cardNonce.getDescription());
        assertNotNull(cardNonce.getThreeDSecureInfo());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertFalse(cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNotNull(cardNonce.getBinData());
        assertEquals(YES, cardNonce.getBinData().getPrepaid());
        assertEquals(YES, cardNonce.getBinData().getHealthcare());
        assertEquals(NO, cardNonce.getBinData().getDebit());
        assertEquals(YES, cardNonce.getBinData().getDurbinRegulated());
        assertEquals(NO, cardNonce.getBinData().getCommercial());
        assertEquals(YES, cardNonce.getBinData().getPayroll());
        assertEquals("Bank of America", cardNonce.getBinData().getIssuingBank());
        assertEquals("USA", cardNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", cardNonce.getBinData().getProductId());
        assertEquals("unregulated",
                cardNonce.getAuthenticationInsight().getRegulationEnvironment());
        assertEquals("01", cardNonce.getExpirationMonth());
        assertEquals("2020", cardNonce.getExpirationYear());
        assertEquals("Joe Smith", cardNonce.getCardholderName());
    }

    @Test
    public void fromJSON_withGraphQLTokenizationResponse_parsesCardNonceWithDefaultValues() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD_MISSING_VALUES));

        assertEquals(PaymentMethodType.CARD, cardNonce.getType());
        assertEquals("", cardNonce.getLastFour());
        assertEquals("", cardNonce.getLastTwo());
        assertEquals("Unknown", cardNonce.getTypeLabel());
        assertEquals("", cardNonce.getDescription());
        assertEquals("Unknown", cardNonce.getCardType());
        assertNotNull(cardNonce.getThreeDSecureInfo());
        assertEquals("", cardNonce.getBin());
        assertNotNull(cardNonce.getBinData());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.getString());
        assertFalse(cardNonce.isDefault());
        assertNull(cardNonce.getAuthenticationInsight());
        assertEquals("", cardNonce.getExpirationMonth());
        assertEquals("", cardNonce.getExpirationYear());
        assertEquals("", cardNonce.getCardholderName());
    }

    @Test
    public void fromJSON_withGraphQLTokenizationResponse_parsesUnknownCardResponses() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.GRAPHQL_RESPONSE_UNKNOWN_CREDIT_CARD));

        assertEquals(PaymentMethodType.CARD, cardNonce.getType());
        assertEquals("Unknown", cardNonce.getCardType());
        assertEquals("tokencc_3bbd22_fpjshh_bqbvh5_mkf3nf_smz", cardNonce.getString());
        assertEquals("", cardNonce.getLastTwo());
        assertEquals("", cardNonce.getLastFour());
        assertEquals("Unknown", cardNonce.getTypeLabel());
        assertEquals("", cardNonce.getDescription());
        assertEquals("", cardNonce.getExpirationMonth());
        assertEquals("", cardNonce.getExpirationYear());
        assertEquals("", cardNonce.getCardholderName());
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
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD));

        Parcel parcel = Parcel.obtain();
        cardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        CardNonce parceled = CardNonce.CREATOR.createFromParcel(parcel);

        assertEquals(PaymentMethodType.CARD, parceled.getType());
        assertEquals("Visa", parceled.getCardType());
        assertEquals("123456-12345-12345-a-adfa", parceled.getString());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("1111", parceled.getLastFour());
        assertEquals("Visa", parceled.getTypeLabel());
        assertEquals("1111", parceled.getDescription());
        assertEquals("01", parceled.getExpirationMonth());
        assertEquals("2020", parceled.getExpirationYear());
        assertEquals("Joe Smith", parceled.getCardholderName());
        assertFalse(parceled.isDefault());
        assertBinDataEqual(cardNonce.getBinData(), parceled.getBinData());
        assertEquals(cardNonce.getAuthenticationInsight().getRegulationEnvironment(),
                parceled.getAuthenticationInsight().getRegulationEnvironment());
    }
}