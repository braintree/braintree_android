package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
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

    private ThreeDSecureLookup lookupWithoutVersion;
    private ThreeDSecureLookup lookupWithVersion1;
    private ThreeDSecureLookup lookupWithVersion2;
    private ThreeDSecureLookup lookupWithoutAcsURL;

    @Before
    public void setUp() throws JSONException {
        JSONObject lookupWithoutVersionJSON = new JSONObject(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE).getJSONObject("lookup");
        lookupWithoutVersion = ThreeDSecureLookup.fromJson(lookupWithoutVersionJSON.toString()); // Lookup doesn't contain a 3DS version number

        JSONObject lookupVersionOneJSON = new JSONObject(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE).getJSONObject("lookup");
        lookupWithVersion1 = ThreeDSecureLookup.fromJson(lookupVersionOneJSON.toString());

        JSONObject lookupVersionTwoJSON = new JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE).getJSONObject("lookup");
        lookupWithVersion2 = ThreeDSecureLookup.fromJson(lookupVersionTwoJSON.toString());

        JSONObject lookupWithoutAcsURLJSON = new JSONObject(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL).getJSONObject("lookup");
        lookupWithoutAcsURL = ThreeDSecureLookup.fromJson(lookupWithoutAcsURLJSON.toString());
    }

    @Test
    public void fromJson_parsesCorrectly() {
        assertEquals("https://acs-url/", lookupWithoutVersion.getAcsUrl());
        assertEquals("merchant-descriptor", lookupWithoutVersion.getMd());
        assertEquals("https://term-url/", lookupWithoutVersion.getTermUrl());
        assertEquals("sample-pareq", lookupWithoutVersion.getPareq());
        assertEquals("", lookupWithoutVersion.getThreeDSecureVersion());
        assertEquals("sample-transaction-id", lookupWithoutVersion.getTransactionId());
        assertTrue(lookupWithoutVersion.requiresUserAuthentication());
    }

    @Test
    public void fromJson_whenLookupVersion1_parsesCorrectly() {
        assertEquals("https://acs-url/", lookupWithVersion1.getAcsUrl());
        assertEquals("merchant-descriptor", lookupWithVersion1.getMd());
        assertEquals("https://term-url/", lookupWithVersion1.getTermUrl());
        assertEquals("pareq", lookupWithVersion1.getPareq());
        assertEquals("1.0.2", lookupWithVersion1.getThreeDSecureVersion());
        assertEquals("some-transaction-id", lookupWithVersion1.getTransactionId());
    }

    @Test
    public void fromJson_whenLookupVersion2_parsesCorrectly() {
        assertEquals("https://acs-url/", lookupWithVersion2.getAcsUrl());
        assertEquals("merchant-descriptor", lookupWithVersion2.getMd());
        assertEquals("https://term-url/", lookupWithVersion2.getTermUrl());
        assertEquals("pareq", lookupWithVersion2.getPareq());
        assertEquals("2.1.0", lookupWithVersion2.getThreeDSecureVersion());
        assertEquals("some-transaction-id", lookupWithVersion2.getTransactionId());
    }

    @Test
    public void fromJson_whenNoAcsURL_parsesCorrectly() {
        assertNull(lookupWithoutAcsURL.getAcsUrl());
        assertEquals("merchant-descriptor", lookupWithoutAcsURL.getMd());
        assertEquals("https://term-url/", lookupWithoutAcsURL.getTermUrl());
        assertEquals("pareq", lookupWithoutAcsURL.getPareq());
        assertFalse(lookupWithoutAcsURL.requiresUserAuthentication());
    }

    @Test
    public void fromJson_whenPareqNull_parsesCorrectly() throws JSONException {
        JSONObject lookupV2NullPareq = new JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_NULL_PAREQ).getJSONObject("lookup");
        ThreeDSecureLookup sut = ThreeDSecureLookup.fromJson(lookupV2NullPareq.toString());
        assertEquals("",sut.getPareq());
    }

    @Test
    public void fromJson_whenPareqMissing_parsesCorrectly() throws JSONException {
        JSONObject lookupV2MissingPareq = new JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_MISSING_PAREQ).getJSONObject("lookup");
        ThreeDSecureLookup sut = ThreeDSecureLookup.fromJson(lookupV2MissingPareq.toString());
        assertEquals("",sut.getPareq());
    }

    @Test
    public void isParcelable() {
        Parcel parcel = Parcel.obtain();
        lookupWithVersion1.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ThreeDSecureLookup parceled = ThreeDSecureLookup.CREATOR.createFromParcel(parcel);

        assertEquals(lookupWithVersion1.getAcsUrl(), parceled.getAcsUrl());
        assertEquals(lookupWithVersion1.getMd(), parceled.getMd());
        assertEquals(lookupWithVersion1.getTermUrl(), parceled.getTermUrl());
        assertEquals(lookupWithVersion1.getPareq(), parceled.getPareq());
        assertEquals(lookupWithVersion1.getThreeDSecureVersion(), parceled.getThreeDSecureVersion());
        assertEquals(lookupWithVersion1.getTransactionId(), parceled.getTransactionId());
    }
}
