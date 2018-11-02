package com.braintreepayments.api.models;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureLookupUnitTest {

    private ThreeDSecureLookup mLookupVersion1;
    private ThreeDSecureLookup mLookup;

    @Before
    public void setUp() throws JSONException {
        mLookupVersion1 = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response.json"));
        mLookup = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_version_2.json"));
    }

    @Test
    public void fromJson_parsesCorrectly() {
        assertEquals("https://acs-url/", mLookupVersion1.getAcsUrl());
        assertEquals("merchant-descriptor", mLookupVersion1.getMd());
        assertEquals("https://term-url/", mLookupVersion1.getTermUrl());
        assertEquals("pareq", mLookupVersion1.getPareq());
        assertEquals("11", mLookupVersion1.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookupVersion1.getCardNonce().getNonce());
        assertEquals("", mLookupVersion1.getThreeDSecureVersion());
        assertEquals("", mLookupVersion1.getTransactionId());
        assertTrue(mLookupVersion1.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookupVersion1.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(mLookupVersion1.getCardNonce().getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void fromJson_whenLookupVersion2_parsesCorrectly() {
        assertEquals("https://acs-url/", mLookup.getAcsUrl());
        assertEquals("merchant-descriptor", mLookup.getMd());
        assertEquals("https://term-url/", mLookup.getTermUrl());
        assertEquals("pareq", mLookup.getPareq());
        assertEquals("11", mLookup.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookup.getCardNonce().getNonce());
        assertEquals("1.0.2", mLookup.getThreeDSecureVersion());
        assertEquals("some-transaction-id", mLookup.getTransactionId());
        assertTrue(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookup.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(mLookup.getCardNonce().getThreeDSecureInfo().wasVerified());
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
        assertEquals(mLookup.getCardNonce().getThreeDSecureInfo().wasVerified(),
                parceled.getCardNonce().getThreeDSecureInfo().wasVerified());
        assertEquals(mLookup.getThreeDSecureVersion(), parceled.getThreeDSecureVersion());
        assertEquals(mLookup.getTransactionId(), parceled.getTransactionId());
    }
}
