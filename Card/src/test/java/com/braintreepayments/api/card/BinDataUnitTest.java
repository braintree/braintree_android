package com.braintreepayments.api.card;

import static com.braintreepayments.api.testutils.Assertions.assertBinDataEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import android.os.Parcel;

import com.braintreepayments.api.testutils.Fixtures;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BinDataUnitTest {

    @Test
    public void parsesCorrectly_ifEmptyJson() throws JSONException {
        BinData binData = BinData.fromJson(new JSONObject("{}"));

        assertNotNull(binData);
        assertEquals(BinData.BinType.Unknown, binData.getPrepaid());
        assertEquals(BinData.BinType.Unknown, binData.getHealthcare());
        assertEquals(BinData.BinType.Unknown, binData.getDebit());
        assertEquals(BinData.BinType.Unknown, binData.getDurbinRegulated());
        assertEquals(BinData.BinType.Unknown, binData.getCommercial());
        assertEquals(BinData.BinType.Unknown, binData.getPayroll());
        assertEquals("", binData.getIssuingBank());
        assertEquals("", binData.getCountryOfIssuance());
        assertEquals("", binData.getProductId());
    }

    @Test
    public void parsesCorrectly_ifNull() {
        BinData binData = BinData.fromJson(new JSONObject());

        assertNotNull(binData);
        assertEquals(BinData.BinType.Unknown, binData.getPrepaid());
        assertEquals(BinData.BinType.Unknown, binData.getHealthcare());
        assertEquals(BinData.BinType.Unknown, binData.getDebit());
        assertEquals(BinData.BinType.Unknown, binData.getDurbinRegulated());
        assertEquals(BinData.BinType.Unknown, binData.getCommercial());
        assertEquals(BinData.BinType.Unknown, binData.getPayroll());
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
        assertEquals(BinData.BinType.Unknown.name(), binData.getIssuingBank());
        assertEquals(BinData.BinType.Unknown.name(), binData.getCountryOfIssuance());
        assertEquals(BinData.BinType.Unknown.name(), binData.getProductId());
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
