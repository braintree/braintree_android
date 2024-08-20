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
        assertEquals(BinType.Unknown, binData.getPrepaid());
        assertEquals(BinType.Unknown, binData.getHealthcare());
        assertEquals(BinType.Unknown, binData.getDebit());
        assertEquals(BinType.Unknown, binData.getDurbinRegulated());
        assertEquals(BinType.Unknown, binData.getCommercial());
        assertEquals(BinType.Unknown, binData.getPayroll());
        assertEquals("", binData.getIssuingBank());
        assertEquals("", binData.getCountryOfIssuance());
        assertEquals("", binData.getProductId());
    }

    @Test
    public void parsesCorrectly_ifNull() {
        BinData binData = BinData.fromJson(new JSONObject());

        assertNotNull(binData);
        assertEquals(BinType.Unknown, binData.getPrepaid());
        assertEquals(BinType.Unknown, binData.getHealthcare());
        assertEquals(BinType.Unknown, binData.getDebit());
        assertEquals(BinType.Unknown, binData.getDurbinRegulated());
        assertEquals(BinType.Unknown, binData.getCommercial());
        assertEquals(BinType.Unknown, binData.getPayroll());
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
        assertEquals(BinType.Unknown.name(), binData.getIssuingBank());
        assertEquals(BinType.Unknown.name(), binData.getCountryOfIssuance());
        assertEquals(BinType.Unknown.name(), binData.getProductId());
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
