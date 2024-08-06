package com.braintreepayments.api.threedsecure;

import static com.braintreepayments.api.testutils.Assertions.assertBinDataEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import android.os.Parcel;

import com.braintreepayments.api.card.BinData;
import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureNonceUnitTest {
    @Test
    public void fromJSON_parsesThreeDSecureNonce() throws JSONException {
        ThreeDSecureNonce threeDSecureNonce = ThreeDSecureNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD));

        assertEquals("Visa", threeDSecureNonce.getCardType());
        assertEquals("123456-12345-12345-a-adfa", threeDSecureNonce.getString());
        assertEquals("11", threeDSecureNonce.getLastTwo());
        assertEquals("1111", threeDSecureNonce.getLastFour());
        assertNotNull(threeDSecureNonce.getThreeDSecureInfo());
        assertFalse(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNotNull(threeDSecureNonce.getBinData());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getPrepaid());
        assertEquals(BinData.BinType.Yes, threeDSecureNonce.getBinData().getHealthcare());
        assertEquals(BinData.BinType.No, threeDSecureNonce.getBinData().getDebit());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getDurbinRegulated());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getCommercial());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getPayroll());
        assertEquals(BinData.BinType.Unknown.name(), threeDSecureNonce.getBinData().getIssuingBank());
        assertEquals("Something", threeDSecureNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", threeDSecureNonce.getBinData().getProductId());
        assertEquals("unregulated",
                threeDSecureNonce.getAuthenticationInsight().getRegulationEnvironment());
        assertEquals("01", threeDSecureNonce.getExpirationMonth());
        assertEquals("2020", threeDSecureNonce.getExpirationYear());
        assertEquals("Joe Smith", threeDSecureNonce.getCardholderName());
    }

    @Test
    public void fromJSON_withGraphQLTokenizationResponse_parsesCardNonce() throws JSONException {
        ThreeDSecureNonce threeDSecureNonce = ThreeDSecureNonce.fromJSON(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD));

        assertEquals("Visa", threeDSecureNonce.getCardType());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", threeDSecureNonce.getString());
        assertEquals("11", threeDSecureNonce.getLastTwo());
        assertEquals("1111", threeDSecureNonce.getLastFour());
        assertNotNull(threeDSecureNonce.getThreeDSecureInfo());
        assertFalse(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertFalse(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNotNull(threeDSecureNonce.getBinData());
        assertEquals(BinData.BinType.Yes, threeDSecureNonce.getBinData().getPrepaid());
        assertEquals(BinData.BinType.Yes, threeDSecureNonce.getBinData().getHealthcare());
        assertEquals(BinData.BinType.No, threeDSecureNonce.getBinData().getDebit());
        assertEquals(BinData.BinType.Yes, threeDSecureNonce.getBinData().getDurbinRegulated());
        assertEquals(BinData.BinType.No, threeDSecureNonce.getBinData().getCommercial());
        assertEquals(BinData.BinType.Yes, threeDSecureNonce.getBinData().getPayroll());
        assertEquals("Bank of America", threeDSecureNonce.getBinData().getIssuingBank());
        assertEquals("USA", threeDSecureNonce.getBinData().getCountryOfIssuance());
        assertEquals("123", threeDSecureNonce.getBinData().getProductId());
        assertEquals("unregulated",
                threeDSecureNonce.getAuthenticationInsight().getRegulationEnvironment());
        assertEquals("01", threeDSecureNonce.getExpirationMonth());
        assertEquals("2020", threeDSecureNonce.getExpirationYear());
        assertEquals("Joe Smith", threeDSecureNonce.getCardholderName());
    }

    @Test
    public void fromJSON_withGraphQLTokenizationResponse_parsesThreeDSecureWithDefaultValues() throws JSONException {
        ThreeDSecureNonce threeDSecureNonce = ThreeDSecureNonce.fromJSON(new JSONObject(Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD_MISSING_VALUES));

        assertEquals("", threeDSecureNonce.getLastFour());
        assertEquals("", threeDSecureNonce.getLastTwo());
        assertEquals("Unknown", threeDSecureNonce.getCardType());
        assertNotNull(threeDSecureNonce.getThreeDSecureInfo());
        assertEquals("", threeDSecureNonce.getBin());
        assertNotNull(threeDSecureNonce.getBinData());
        assertEquals("3744a73e-b1ab-0dbd-85f0-c12a0a4bd3d1", threeDSecureNonce.getString());
        assertFalse(threeDSecureNonce.isDefault());
        assertNull(threeDSecureNonce.getAuthenticationInsight());
        assertEquals("", threeDSecureNonce.getExpirationMonth());
        assertEquals("", threeDSecureNonce.getExpirationYear());
        assertEquals("", threeDSecureNonce.getCardholderName());
    }

    @Test
    public void fromJSON_withGraphQLTokenizationResponse_parsesUnknownCardResponses() throws JSONException {
        ThreeDSecureNonce threeDSecureNonce = ThreeDSecureNonce.fromJSON(new JSONObject(Fixtures.GRAPHQL_RESPONSE_UNKNOWN_CREDIT_CARD));

        assertEquals("Unknown", threeDSecureNonce.getCardType());
        assertEquals("tokencc_3bbd22_fpjshh_bqbvh5_mkf3nf_smz", threeDSecureNonce.getString());
        assertEquals("", threeDSecureNonce.getLastTwo());
        assertEquals("", threeDSecureNonce.getLastFour());
        assertEquals("", threeDSecureNonce.getExpirationMonth());
        assertEquals("", threeDSecureNonce.getExpirationYear());
        assertEquals("", threeDSecureNonce.getCardholderName());
        assertNotNull(threeDSecureNonce.getThreeDSecureInfo());
        assertFalse(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShifted());
        assertFalse(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShiftPossible());
        assertNotNull(threeDSecureNonce.getBinData());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getPrepaid());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getHealthcare());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getDebit());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getDurbinRegulated());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getCommercial());
        assertEquals(BinData.BinType.Unknown, threeDSecureNonce.getBinData().getPayroll());
        assertEquals(BinData.BinType.Unknown.name(), threeDSecureNonce.getBinData().getIssuingBank());
        assertEquals(BinData.BinType.Unknown.name(), threeDSecureNonce.getBinData().getCountryOfIssuance());
        assertEquals(BinData.BinType.Unknown.name(), threeDSecureNonce.getBinData().getProductId());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        ThreeDSecureNonce threeDSecureNonce = ThreeDSecureNonce.fromJSON(new JSONObject(Fixtures.PAYMENT_METHODS_RESPONSE_VISA_CREDIT_CARD));

        Parcel parcel = Parcel.obtain();
        threeDSecureNonce.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureNonce parceled = ThreeDSecureNonce.CREATOR.createFromParcel(parcel);

        assertEquals("Visa", parceled.getCardType());
        assertEquals("123456-12345-12345-a-adfa", parceled.getString());
        assertEquals("11", parceled.getLastTwo());
        assertEquals("1111", parceled.getLastFour());
        assertEquals("01", parceled.getExpirationMonth());
        assertEquals("2020", parceled.getExpirationYear());
        assertEquals("Joe Smith", parceled.getCardholderName());
        assertFalse(parceled.isDefault());
        assertBinDataEqual(threeDSecureNonce.getBinData(), parceled.getBinData());
        assertEquals(threeDSecureNonce.getAuthenticationInsight().getRegulationEnvironment(),
                parceled.getAuthenticationInsight().getRegulationEnvironment());
        assertEquals(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShifted(), parceled.getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(threeDSecureNonce.getThreeDSecureInfo().isLiabilityShiftPossible(), parceled.getThreeDSecureInfo().isLiabilityShiftPossible());
    }
}
