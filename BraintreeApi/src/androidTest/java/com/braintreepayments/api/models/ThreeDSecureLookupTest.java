package com.braintreepayments.api.models;

import android.os.Parcel;
import android.test.AndroidTestCase;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;

public class ThreeDSecureLookupTest extends AndroidTestCase {

    private ThreeDSecureLookup mLookup;

    @Override
    protected void setUp() throws Exception {
        mLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response.json"));
    }

    public void testCanInstantiateFromJsonString() {
        assertEquals("https://acs-url/", mLookup.getAcsUrl());
        assertEquals("merchant-descriptor", mLookup.getMd());
        assertEquals("https://term-url/", mLookup.getTermUrl());
        assertEquals("pareq", mLookup.getPareq());
        assertEquals("11", mLookup.getCard().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookup.getCard().getNonce());
        assertTrue(mLookup.getCard().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookup.getCard().getThreeDSecureInfo().isLiabilityShiftPossible());
    }

    public void testCanBeSerialized() {
        Parcel parcel = Parcel.obtain();
        mLookup.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureLookup parsedLookup = ThreeDSecureLookup.CREATOR.createFromParcel(parcel);

        assertEquals(mLookup.getAcsUrl(), parsedLookup.getAcsUrl());
        assertEquals(mLookup.getMd(), parsedLookup.getMd());
        assertEquals(mLookup.getTermUrl(), parsedLookup.getTermUrl());
        assertEquals(mLookup.getPareq(), parsedLookup.getPareq());
        assertEquals(mLookup.getCard().getLastTwo(), parsedLookup.getCard().getLastTwo());
        assertEquals(mLookup.getCard().getNonce(), parsedLookup.getCard().getNonce());
        assertEquals(mLookup.getCard().getThreeDSecureInfo().isLiabilityShifted(),
                parsedLookup.getCard().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(mLookup.getCard().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parsedLookup.getCard().getThreeDSecureInfo().isLiabilityShiftPossible());
    }

}
