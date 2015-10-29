package com.braintreepayments.api.models;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ThreeDSecureLookupTest {

    private ThreeDSecureLookup mLookup;

    @Before
    public void setUp() throws JSONException {
        mLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response.json"));
    }

    @Test(timeout = 1000)
    @SmallTest
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

    @Test(timeout = 1000)
    @SmallTest
    public void isParcelable() {
        Parcel parcel = Parcel.obtain();
        mLookup.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureLookup parsedLookup = ThreeDSecureLookup.CREATOR.createFromParcel(parcel);

        assertEquals(mLookup.getAcsUrl(), parsedLookup.getAcsUrl());
        assertEquals(mLookup.getMd(), parsedLookup.getMd());
        assertEquals(mLookup.getTermUrl(), parsedLookup.getTermUrl());
        assertEquals(mLookup.getPareq(), parsedLookup.getPareq());
        assertEquals(mLookup.getCardNonce().getLastTwo(), parsedLookup.getCardNonce().getLastTwo());
        assertEquals(mLookup.getCardNonce().getNonce(), parsedLookup.getCardNonce().getNonce());
        assertEquals(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(),
                parsedLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parsedLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
    }
}
