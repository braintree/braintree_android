package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class IdealResultUnitTest {

    @Test
    public void canCreateIdealResultFromJson() throws JSONException {
        IdealResult bankNonce = IdealResult.fromJson(stringFromFixture("payment_methods/completed_ideal_bank_payment.json"));

        assertEquals("ideal_payment_id", bankNonce.getId());
        assertEquals("short_id", bankNonce.getShortId());
        assertEquals("COMPLETE", bankNonce.getStatus());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        IdealResult result = IdealResult.fromJson(stringFromFixture("payment_methods/completed_ideal_bank_payment.json"));
        Parcel parcel = Parcel.obtain();
        result.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        IdealResult parceled = IdealResult.CREATOR.createFromParcel(parcel);

        assertEquals("ideal_payment_id", parceled.getId());
        assertEquals("short_id", parceled.getShortId());
        assertEquals("COMPLETE", parceled.getStatus());
    }
}
