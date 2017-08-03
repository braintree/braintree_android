package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class BinDataUnitTest {

    @Test
    public void isParcelable() throws JSONException {
        CardNonce cardNonce = CardNonce.fromJson(stringFromFixture("payment_methods/visa_credit_card_response_with_bin_data.json"));
        BinData binData = cardNonce.getBinData();
        Parcel parcel = Parcel.obtain();
        binData.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        BinData parceled = BinData.CREATOR.createFromParcel(parcel);

        assertEquals(binData.getPrepaid(), parceled.getPrepaid());
        assertEquals(binData.getHealthcare(), parceled.getHealthcare());
        assertEquals(binData.getDebit(), parceled.getDebit());
        assertEquals(binData.getDurbinRegulated(), parceled.getDurbinRegulated());
        assertEquals(binData.getCommercial(), parceled.getCommercial());
        assertEquals(binData.getPayroll(), parceled.getPayroll());
        assertEquals(binData.getIssuingBank(), parceled.getIssuingBank());
        assertEquals(binData.getCountryOfIssuance(), parceled.getCountryOfIssuance());
        assertEquals(binData.getProductId(), parceled.getProductId());
    }
}
