package com.braintreepayments.api.threedsecure

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class ThreeDSecureLookupTest {

    @Test
    fun fromJson_parsesAllFields() {
        val lookupJson = JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
            .getJSONObject("lookup").toString()

        val lookup = ThreeDSecureLookup.fromJson(lookupJson)

        assertEquals("https://acs-url/", lookup.acsUrl)
        assertEquals("merchant-descriptor", lookup.md)
        assertEquals("https://term-url/", lookup.termUrl)
        assertEquals("pareq", lookup.pareq)
        assertEquals("2.1.0", lookup.threeDSecureVersion)
        assertEquals("some-transaction-id", lookup.transactionId)
        assertTrue(lookup.requiresUserAuthentication())
    }

    @Test
    fun fromJson_withNullPareq_returnsEmptyString() {
        val lookupJson = JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_NULL_PAREQ)
            .getJSONObject("lookup").toString()

        assertEquals("", ThreeDSecureLookup.fromJson(lookupJson).pareq)
    }

    @Test
    fun fromJson_withMissingPareq_returnsEmptyString() {
        val lookupJson = JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE_MISSING_PAREQ)
            .getJSONObject("lookup").toString()

        assertEquals("", ThreeDSecureLookup.fromJson(lookupJson).pareq)
    }

    @Test
    fun fromJson_withNoAcsUrl_requiresUserAuthenticationReturnsFalse() {
        val lookupJson = JSONObject(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL)
            .getJSONObject("lookup").toString()

        val lookup = ThreeDSecureLookup.fromJson(lookupJson)

        assertNull(lookup.acsUrl)
        assertFalse(lookup.requiresUserAuthentication())
    }

    @Test
    fun parcels_correctly() {
        val lookupJson = JSONObject(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
            .getJSONObject("lookup").toString()
        val original = ThreeDSecureLookup.fromJson(lookupJson)

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<ThreeDSecureLookup>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(original, restored)
    }
}
