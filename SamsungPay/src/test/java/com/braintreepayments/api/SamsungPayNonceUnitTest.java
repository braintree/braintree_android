package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SamsungPayNonceUnitTest {

    @Test
    public void fromJSON_parsesNonce_v2() throws JSONException {
        SamsungPayNonce sut = SamsungPayNonce.fromJSON(new JSONObject(Fixtures.SAMSUNG_PAY_RESPONSE_V2));

        assertEquals("tokensam_bf_v8s9hv_2htw4m_nh4f45_y3hsft_wty", sut.getString());
        assertFalse(sut.isDefault());

        assertEquals("Mastercard", sut.getCardType());
        assertEquals("1798", sut.getLastFour());

        BinData binData = sut.getBinData();
        assertEquals("Unknown", binData.getPrepaid());
        assertEquals("Yes", binData.getHealthcare());
        assertEquals("No", binData.getDebit());
        assertEquals("Unknown", binData.getDurbinRegulated());
        assertEquals("Unknown", binData.getCommercial());
        assertEquals("Unknown", binData.getPayroll());
        assertEquals("Unknown", binData.getIssuingBank());
        assertEquals("US", binData.getCountryOfIssuance());
        assertEquals("123", binData.getProductId());
    }

    @Test
    public void fromJSON_parsesNonce_v1() throws JSONException {
        SamsungPayNonce sut = SamsungPayNonce.fromJSON(new JSONObject(Fixtures.SAMSUNG_PAY_RESPONSE_V1));

        assertEquals("tokensam_bf_v8s9hv_2htw4m_nh4f45_y3hsft_wty", sut.getString());
        assertFalse(sut.isDefault());

        assertEquals("Mastercard", sut.getCardType());
        assertEquals("1798", sut.getLastFour());

        BinData binData = sut.getBinData();
        assertEquals("Unknown", binData.getPrepaid());
        assertEquals("Yes", binData.getHealthcare());
        assertEquals("No", binData.getDebit());
        assertEquals("Unknown", binData.getDurbinRegulated());
        assertEquals("Unknown", binData.getCommercial());
        assertEquals("Unknown", binData.getPayroll());
        assertEquals("Unknown", binData.getIssuingBank());
        assertEquals("US", binData.getCountryOfIssuance());
        assertEquals("123", binData.getProductId());
    }
}