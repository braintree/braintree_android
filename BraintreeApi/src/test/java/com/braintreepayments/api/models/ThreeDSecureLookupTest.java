package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
public class ThreeDSecureLookupTest {

    private ThreeDSecureLookup mLookup;

    @Before
    public void setUp() throws JSONException {
        mLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response.json"));
    }

    @Test
    public void fromJson_parsesCorrectly() {
        assertEquals("https://acs-url/", mLookup.getAcsUrl());
        assertEquals("merchant-descriptor", mLookup.getMd());
        assertEquals("https://term-url/", mLookup.getTermUrl());
        assertEquals("pareq", mLookup.getPareq());
        assertEquals("11", mLookup.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookup.getCardNonce().getNonce());
        assertTrue(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
    }

    @Test
    public void isParcelable() {
        Parcel parcel = Parcel.obtain();
        mLookup.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureLookup parceled = ThreeDSecureLookup.CREATOR.createFromParcel(parcel);

        assertEquals(mLookup.getAcsUrl(), parceled.getAcsUrl());
        assertEquals(mLookup.getMd(), parceled.getMd());
        assertEquals(mLookup.getTermUrl(), parceled.getTermUrl());
        assertEquals(mLookup.getPareq(), parceled.getPareq());
        assertEquals(mLookup.getCardNonce().getLastTwo(), parceled.getCardNonce().getLastTwo());
        assertEquals(mLookup.getCardNonce().getNonce(), parceled.getCardNonce().getNonce());
        assertEquals(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
    }
}
