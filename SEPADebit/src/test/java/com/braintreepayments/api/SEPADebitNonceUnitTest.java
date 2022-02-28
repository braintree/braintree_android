package com.braintreepayments.api;

import static junit.framework.TestCase.assertEquals;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SEPADebitNonceUnitTest {

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        SEPADebitNonce sut = SEPADebitNonce.fromJSON(new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));

        assertEquals("1194c322-9763-08b7-4777-0b9b5e5cc3e4", sut.getString());
        assertEquals("1234", sut.getIbanLastFour());
        assertEquals("a-customer-id", sut.getCustomerId());
        assertEquals(SEPADebitMandateType.ONE_OFF, sut.getMandateType());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        SEPADebitNonce sut = SEPADebitNonce.fromJSON(new JSONObject(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE));
        Parcel parcel = Parcel.obtain();
        sut.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        SEPADebitNonce parceled = SEPADebitNonce.CREATOR.createFromParcel(parcel);

        assertEquals("1194c322-9763-08b7-4777-0b9b5e5cc3e4", parceled.getString());
        assertEquals("1234", parceled.getIbanLastFour());
        assertEquals("a-customer-id", parceled.getCustomerId());
        assertEquals(SEPADebitMandateType.ONE_OFF, parceled.getMandateType());
    }
}
