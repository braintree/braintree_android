package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(RobolectricTestRunner.class)
public class ThreeDSecureLookupUnitTest {

    private ThreeDSecureLookup mLookupWithoutVersion;
    private ThreeDSecureLookup mLookupWithVersion1;
    private ThreeDSecureLookup mLookupWithVersion2;
    private ThreeDSecureLookup mLookupWithoutAcsURL;

    @Before
    public void setUp() throws JSONException {
        mLookupWithoutVersion = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE); // Lookup doesn't contain a 3DS version number
        mLookupWithVersion1 = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE);
        mLookupWithVersion2 = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE);
        mLookupWithoutAcsURL = ThreeDSecureLookup.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL);
    }

    @Test
    public void fromJson_parsesCorrectly() {
        assertEquals("https://acs-url/", mLookupWithoutVersion.getAcsUrl());
        assertEquals("merchant-descriptor", mLookupWithoutVersion.getMd());
        assertEquals("https://term-url/", mLookupWithoutVersion.getTermUrl());
        assertEquals("sample-pareq", mLookupWithoutVersion.getPareq());
        assertEquals("11", mLookupWithoutVersion.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookupWithoutVersion.getCardNonce().getNonce());
        assertEquals("", mLookupWithoutVersion.getThreeDSecureVersion());
        assertEquals("sample-transaction-id", mLookupWithoutVersion.getTransactionId());
        assertTrue(mLookupWithoutVersion.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookupWithoutVersion.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(mLookupWithoutVersion.getCardNonce().getThreeDSecureInfo().wasVerified());
        assertTrue(mLookupWithoutVersion.requiresUserAuthentication());
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
    public void fromJson_whenNoAcsURL_parsesCorrectly() {
        assertNull(mLookupWithoutAcsURL.getAcsUrl());
        assertEquals("merchant-descriptor", mLookupWithoutAcsURL.getMd());
        assertEquals("https://term-url/", mLookupWithoutAcsURL.getTermUrl());
        assertEquals("pareq", mLookupWithoutAcsURL.getPareq());
        assertEquals("11", mLookupWithoutAcsURL.getCardNonce().getLastTwo());
        assertEquals("123456-12345-12345-a-adfa", mLookupWithoutAcsURL.getCardNonce().getNonce());
        assertTrue(mLookupWithoutAcsURL.getCardNonce().getThreeDSecureInfo().isLiabilityShifted());
        assertTrue(mLookupWithoutAcsURL.getCardNonce().getThreeDSecureInfo().isLiabilityShiftPossible());
        assertTrue(mLookupWithoutAcsURL.getCardNonce().getThreeDSecureInfo().wasVerified());
        assertFalse(mLookupWithoutAcsURL.requiresUserAuthentication());
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
