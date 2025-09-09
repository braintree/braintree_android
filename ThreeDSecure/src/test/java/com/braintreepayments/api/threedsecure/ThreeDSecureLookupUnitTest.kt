package com.braintreepayments.api.threedsecure

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.threedsecure.ThreeDSecureLookup.Companion.fromJson
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertFalse
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureLookupUnitTest {

    private lateinit var lookupWithoutVersion: ThreeDSecureLookup
    private lateinit var lookupWithVersion1: ThreeDSecureLookup
    private lateinit var lookupWithVersion2: ThreeDSecureLookup
    private lateinit var lookupWithoutAcsURL: ThreeDSecureLookup

    @Before
    @Throws(JSONException::class)
    fun setUp() {
        val lookupWithoutVersionJSON = JSONObject(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
                                        .getJSONObject("lookup")
        lookupWithoutVersion = fromJson(lookupWithoutVersionJSON.toString())

        val lookupVersionOneJSON = JSONObject(Fixtures.THREE_D_SECURE_V1_LOOKUP_RESPONSE)
                                    .getJSONObject("lookup")
        lookupWithVersion1 = fromJson(lookupVersionOneJSON.toString())

        val lookupVersionTwoJSON = JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
                                    .getJSONObject("lookup")
        lookupWithVersion2 = fromJson(lookupVersionTwoJSON.toString())

        val lookupWithoutAcsURLJSON = JSONObject(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL)
                                        .getJSONObject("lookup")
        lookupWithoutAcsURL = fromJson(lookupWithoutAcsURLJSON.toString())
    }

    @Test
    fun `parses lookupWithoutVersionJSON created from JSON correctly`() {
        assertEquals("https://acs-url/", lookupWithoutVersion.acsUrl)
        assertEquals("merchant-descriptor", lookupWithoutVersion.md)
        assertEquals("https://term-url/", lookupWithoutVersion.termUrl)
        assertEquals("sample-pareq", lookupWithoutVersion.pareq)
        assertEquals("2.1.0", lookupWithoutVersion.threeDSecureVersion)
        assertEquals("sample-transaction-id", lookupWithoutVersion.transactionId)
        assertTrue(lookupWithoutVersion.requiresUserAuthentication())
    }

    @Test
    fun `parses lookupWithVersion1 created from JSON correctly`() {
        assertEquals("https://acs-url/", lookupWithVersion1.acsUrl)
        assertEquals("merchant-descriptor", lookupWithVersion1.md)
        assertEquals("https://term-url/", lookupWithVersion1.termUrl)
        assertEquals("pareq", lookupWithVersion1.pareq)
        assertEquals("1.0.2", lookupWithVersion1.threeDSecureVersion)
        assertEquals("some-transaction-id", lookupWithVersion1.transactionId)
    }

    @Test
    fun `parses lookupWithVersion2 created from JSON correctly`() {
        assertEquals("https://acs-url/", lookupWithVersion2.acsUrl)
        assertEquals("merchant-descriptor", lookupWithVersion2.md)
        assertEquals("https://term-url/", lookupWithVersion2.termUrl)
        assertEquals("pareq", lookupWithVersion2.pareq)
        assertEquals("2.1.0", lookupWithVersion2.threeDSecureVersion)
        assertEquals("some-transaction-id", lookupWithVersion2.transactionId)
    }

    @Test
    fun `parses lookupWithoutAcsURL created from JSON correctly`() {
        assertNull(lookupWithoutAcsURL.acsUrl)
        assertEquals("merchant-descriptor", lookupWithoutAcsURL.md)
        assertEquals("https://term-url/", lookupWithoutAcsURL.termUrl)
        assertEquals("pareq", lookupWithoutAcsURL.pareq)
        assertFalse(lookupWithoutAcsURL.requiresUserAuthentication())
    }

    @Test
    @Throws(JSONException::class)
    fun `creates ThreeDSecureLookup from JSON with pareq being null and parses it correctly`() {
        val lookupV2NullPareq = JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_NULL_PAREQ)
                                .getJSONObject("lookup")
        val sut = fromJson(lookupV2NullPareq.toString())
        assertEquals("", sut.pareq)
    }

    @Test
    @Throws(JSONException::class)
    fun `creates ThreeDSecureLookup from JSON with pareq missing and parses it correctly`() {
        val lookupV2MissingPareq = JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_MISSING_PAREQ)
                                    .getJSONObject("lookup")
        val sut = fromJson(lookupV2MissingPareq.toString())
        assertEquals("", sut.pareq)
    }

    @Test
    fun `parcels and unparcels ThreeDSecureLookup object correctly`() {
        val parcel = Parcel.obtain().apply {
            lookupWithVersion1.writeToParcel(this, 0)
            setDataPosition(0)
        }

        val parceled = parcelableCreator<ThreeDSecureLookup>().createFromParcel(parcel)

        assertEquals(lookupWithVersion1.acsUrl, parceled.acsUrl)
        assertEquals(lookupWithVersion1.md, parceled.md)
        assertEquals(lookupWithVersion1.termUrl, parceled.termUrl)
        assertEquals(lookupWithVersion1.pareq, parceled.pareq)
        assertEquals(lookupWithVersion1.threeDSecureVersion, parceled.threeDSecureVersion)
        assertEquals(lookupWithVersion1.transactionId, parceled.transactionId)
    }
}
