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

    private ThreeDSecureLookup mLookupWithoutVersion;
    private ThreeDSecureLookup mLookupWithVersion1;
    private ThreeDSecureLookup mLookupWithVersion2;

    @Before
    public void setUp() throws JSONException {
        mLookupWithoutVersion = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response.json")); // Lookup doesn't contain a 3DS version number
        mLookupWithVersion1 = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number1.json"));
        mLookupWithVersion2 = ThreeDSecureLookup.fromJson(stringFromFixture("three_d_secure/lookup_response_with_version_number2.json"));
    }

    @Test
    public void fromJson_parsesCorrectly() {
        assertEquals("https://acs-url/", mLookupWithoutVersion.getAcsUrl());
        assertEquals("merchant-descriptor", mLookupWithoutVersion.getMd());
        assertEquals("https://term-url/", mLookupWithoutVersion.getTermUrl());
        assertEquals("pareq", mLookupWithoutVersion.getPareq());
        assertEquals("11", mLookupWithoutVersion.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookupWithoutVersion.getCardNonce().getNonce());
        assertEquals("", mLookupWithoutVersion.getThreeDSecureVersion());
        assertEquals("", mLookupWithoutVersion.getTransactionId());
        assertTrue(mLookupWithoutVersion.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookupWithoutVersion.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(mLookupWithoutVersion.getCardNonce().getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void fromJson_whenLookupVersion1_parsesCorrectly() {
        assertEquals("https://acs-url/", mLookupWithVersion1.getAcsUrl());
        assertEquals("merchant-descriptor", mLookupWithVersion1.getMd());
        assertEquals("https://term-url/", mLookupWithVersion1.getTermUrl());
        assertEquals("pareq", mLookupWithVersion1.getPareq());
        assertEquals("11", mLookupWithVersion1.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookupWithVersion1.getCardNonce().getNonce());
        assertEquals("1.0.2", mLookupWithVersion1.getThreeDSecureVersion());
        assertEquals("some-transaction-id", mLookupWithVersion1.getTransactionId());
        assertTrue(mLookupWithVersion1.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookupWithVersion1.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(mLookupWithVersion1.getCardNonce().getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void fromJson_whenLookupVersion2_parsesCorrectly() {
        assertEquals("https://acs-url/", mLookupWithVersion2.getAcsUrl());
        assertEquals("merchant-descriptor", mLookupWithVersion2.getMd());
        assertEquals("https://term-url/", mLookupWithVersion2.getTermUrl());
        assertEquals("pareq", mLookupWithVersion2.getPareq());
        assertEquals("11", mLookupWithVersion2.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookupWithVersion2.getCardNonce().getNonce());
        assertEquals("2.1.0", mLookupWithVersion2.getThreeDSecureVersion());
        assertEquals("some-transaction-id", mLookupWithVersion2.getTransactionId());
        assertTrue(mLookupWithVersion2.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookupWithVersion2.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(mLookupWithVersion2.getCardNonce().getThreeDSecureInfo().wasVerified());
    }

    @Test
    public void fromJson_isThreeDSecureVersion2CheckPasses() {
        assertTrue(mLookupWithVersion2.isThreeDSecureVersion2());
    }

    @Test
    public void isParcelable() {
        Parcel parcel = Parcel.obtain();
        mLookupWithVersion1.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureLookup parceled = ThreeDSecureLookup.CREATOR.createFromParcel(parcel);

        assertEquals(mLookupWithVersion1.getAcsUrl(), parceled.getAcsUrl());
        assertEquals(mLookupWithVersion1.getMd(), parceled.getMd());
        assertEquals(mLookupWithVersion1.getTermUrl(), parceled.getTermUrl());
        assertEquals(mLookupWithVersion1.getPareq(), parceled.getPareq());
        assertEquals(mLookupWithVersion1.getCardNonce().getLastTwo(), parceled.getCardNonce().getLastTwo());
        assertEquals(mLookupWithVersion1.getCardNonce().getNonce(), parceled.getCardNonce().getNonce());
        assertEquals(mLookupWithVersion1.getCardNonce().getThreeDSecureInfo().isLiabilityShifted(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertEquals(mLookupWithVersion1.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible(),
                parceled.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertEquals(mLookupWithVersion1.getCardNonce().getThreeDSecureInfo().wasVerified(),
                parceled.getCardNonce().getThreeDSecureInfo().wasVerified());
        assertEquals(mLookupWithVersion1.getThreeDSecureVersion(), parceled.getThreeDSecureVersion());
        assertEquals(mLookupWithVersion1.getTransactionId(), parceled.getTransactionId());
    }
}
