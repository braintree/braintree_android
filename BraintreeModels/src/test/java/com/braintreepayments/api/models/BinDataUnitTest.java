package com.braintreepayments.api.models;

import android.os.Parcel;

import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.api.models.BinData.UNKNOWN;
import static com.braintreepayments.testutils.Assertions.assertBinDataEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class BinDataUnitTest {

    @Test
    public void parsesCorrectly_ifEmptyJson() throws JSONException {
        BinData binData = BinData.fromJson(new JSONObject("{}"));

        assertNotNull(binData);
        assertEquals(UNKNOWN, binData.getPrepaid());
        assertEquals(UNKNOWN, binData.getHealthcare());
        assertEquals(UNKNOWN, binData.getDebit());
        assertEquals(UNKNOWN, binData.getDurbinRegulated());
        assertEquals(UNKNOWN, binData.getCommercial());
        assertEquals(UNKNOWN, binData.getPayroll());
        assertEquals("", binData.getIssuingBank());
        assertEquals("", binData.getCountryOfIssuance());
        assertEquals("", binData.getProductId());
    }

    @Test
    public void parsesCorrectly_ifNull() {
        BinData binData = BinData.fromJson(new JSONObject());

        assertNotNull(binData);
        assertEquals(UNKNOWN, binData.getPrepaid());
        assertEquals(UNKNOWN, binData.getHealthcare());
        assertEquals(UNKNOWN, binData.getDebit());
        assertEquals(UNKNOWN, binData.getDurbinRegulated());
        assertEquals(UNKNOWN, binData.getCommercial());
        assertEquals(UNKNOWN, binData.getPayroll());
        assertEquals("", binData.getIssuingBank());
        assertEquals("", binData.getCountryOfIssuance());
        assertEquals("", binData.getProductId());
    }

    @Test
    public void parsesCorrectly_withNullValues() throws JSONException {
        JSONObject jsonObject = new JSONObject(Fixtures.BIN_DATA)
                .put("issuingBank", JSONObject.NULL)
                .put("countryOfIssuance", JSONObject.NULL)
                .put("productId", JSONObject.NULL);

        BinData binData = BinData.fromJson(jsonObject);
        assertEquals(UNKNOWN, binData.getIssuingBank());
        assertEquals(UNKNOWN, binData.getCountryOfIssuance());
        assertEquals(UNKNOWN, binData.getProductId());
    }

    @Test
    public void isParcelable() throws JSONException {
        BinData binData = BinData.fromJson(new JSONObject(Fixtures.BIN_DATA));
        Parcel parcel = Parcel.obtain();
        binData.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        BinData parceled = BinData.CREATOR.createFromParcel(parcel);

        assertBinDataEqual(binData, parceled);
    }
}
