package com.braintreepayments.api.card

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class AuthenticationInsightUnitTest {
    @Test
    @Throws(JSONException::class)
    fun `parses PSDTWO regulation environment from JSON`() {
        val response = JSONObject().apply {
            put("customerAuthenticationRegulationEnvironment", "psdtwo")
        }
        val authenticationInsight = AuthenticationInsight.fromJson(response)

        assertEquals("psd2", authenticationInsight?.regulationEnvironment)
    }

    @Test
    @Throws(JSONException::class)
    fun `parses unknown regulation environment from JSON and returns lowercase regulationEnvironment`() {
        val response = JSONObject().apply {
            put("customerAuthenticationRegulationEnvironment", "FaKeVaLuE")
        }
        val authenticationInsight = AuthenticationInsight.fromJson(response)

        assertEquals("fakevalue", authenticationInsight?.regulationEnvironment)
    }

    @Test
    @Throws(JSONException::class)
    fun `parses regulation environment key from JSON and returns its lowercase value`() {
        val response = JSONObject().apply {
            put("regulationEnvironment", "UNREGULATED")
        }
        val authenticationInsight = AuthenticationInsight.fromJson(response)

        assertEquals("unregulated", authenticationInsight?.regulationEnvironment)
    }

    @Test
    fun `receives null JSON object and returns null`() {
        assertNull(AuthenticationInsight.fromJson(null))
    }

    @Test
    @Throws(JSONException::class)
    fun `parcels Auth Insight from JSON and returns correct regulation environment`() {
        val response = JSONObject().apply {
            put("regulationEnvironment", "psdtwo")
        }
        val authenticationInsight = AuthenticationInsight.fromJson(response)
        val parcel = Parcel.obtain().apply {
            authenticationInsight?.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val parceledInsight = parcelableCreator<AuthenticationInsight>().createFromParcel(parcel)

        assertEquals(authenticationInsight?.regulationEnvironment, parceledInsight.regulationEnvironment)
    }
}
