package com.braintreepayments.api.card;

import static com.braintreepayments.api.testutils.Assertions.assertBinDataEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import android.os.Parcel;

import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class CardNonceUnitTest {

    @Test
    public void fromJSON_withPlainJSONCardNonce_parsesCardNonce() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHOD_CARD));

        assertEquals("123456-12345-12345-a-adfa", cardNonce.getString());
        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("11", cardNonce.getLastTwo());
        assertEquals("1111", cardNonce.getLastFour());
        assertTrue(cardNonce.isDefault());
    }

    @Test
    public void fromJSON_withRESTfulTokenizationResponse_parsesCardNonce() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD));

        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", cardNonce.getString());
        assertEquals("11", cardNonce.getLastTwo());
        assertEquals("1111", cardNonce.getLastFour());
        assertNotNull(cardNonce.getBinData());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getPrepaid());
        assertEquals(BinType.Yes, cardNonce.getBinData().getHealthcare());
        assertEquals(BinType.No, cardNonce.getBinData().getDebit());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getDurbinRegulated());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getCommercial());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getPayroll());
        assertEquals(BinType.Unknown.name(), cardNonce.getBinData().getIssuingBank());
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

        assertEquals("Visa", cardNonce.getCardType());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", cardNonce.getString());
        assertEquals("11", cardNonce.getLastTwo());
        assertEquals("1111", cardNonce.getLastFour());
        assertNotNull(cardNonce.getBinData());
        assertEquals(BinType.Yes, cardNonce.getBinData().getPrepaid());
        assertEquals(BinType.Yes, cardNonce.getBinData().getHealthcare());
        assertEquals(BinType.No, cardNonce.getBinData().getDebit());
        assertEquals(BinType.Yes, cardNonce.getBinData().getDurbinRegulated());
        assertEquals(BinType.No, cardNonce.getBinData().getCommercial());
        assertEquals(BinType.Yes, cardNonce.getBinData().getPayroll());
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

        assertEquals("", cardNonce.getLastFour());
        assertEquals("", cardNonce.getLastTwo());
        assertEquals("Unknown", cardNonce.getCardType());
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

        assertEquals("Unknown", cardNonce.getCardType());
        assertEquals("tokencc_3bbd22_fpjshh_bqbvh5_mkf3nf_smz", cardNonce.getString());
        assertEquals("", cardNonce.getLastTwo());
        assertEquals("", cardNonce.getLastFour());
        assertEquals("", cardNonce.getExpirationMonth());
        assertEquals("", cardNonce.getExpirationYear());
        assertEquals("", cardNonce.getCardholderName());
        assertNotNull(cardNonce.getBinData());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getPrepaid());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getHealthcare());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getDebit());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getDurbinRegulated());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getCommercial());
        assertEquals(BinType.Unknown, cardNonce.getBinData().getPayroll());
        assertEquals(BinType.Unknown.name(), cardNonce.getBinData().getIssuingBank());
        assertEquals(BinType.Unknown.name(), cardNonce.getBinData().getCountryOfIssuance());
        assertEquals(BinType.Unknown.name(), cardNonce.getBinData().getProductId());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD));

        Parcel parcel = Parcel.obtain();
        cardNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        CardNonce parceled = CardNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Visa", parceled.getCardType());
        assertEquals("123456-12345-12345-a-adfa", parceled.getString());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("1111", parceled.getLastFour());
        assertEquals("01", parceled.getExpirationMonth());
        assertEquals("2020", parceled.getExpirationYear());
        assertEquals("Joe Smith", parceled.getCardholderName());
        assertFalse(parceled.isDefault());
        assertBinDataEqual(cardNonce.getBinData(), parceled.getBinData());
        assertEquals(cardNonce.getAuthenticationInsight().getRegulationEnvironment(),
                parceled.getAuthenticationInsight().getRegulationEnvironment());
    }
}