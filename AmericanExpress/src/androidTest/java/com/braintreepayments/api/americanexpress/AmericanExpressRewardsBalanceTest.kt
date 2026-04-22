package com.braintreepayments.api.americanexpress

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class AmericanExpressRewardsBalanceTest {

    @Test(timeout = 1000)
    fun fromJson_parsesAllFields() {
        val json = JSONObject().apply {
            put("conversionRate", "0.0070")
            put("currencyAmount", "316.00")
            put("currencyIsoCode", "USD")
            put("requestId", "715f4712-8571-42c4-8413-f0d9800f4e09")
            put("rewardsAmount", "45000")
            put("rewardsUnit", "Points")
        }.toString()

        val result = AmericanExpressRewardsBalance.fromJson(json)

        assertEquals("0.0070", result.conversionRate)
        assertEquals("316.00", result.currencyAmount)
        assertEquals("USD", result.currencyIsoCode)
        assertEquals("715f4712-8571-42c4-8413-f0d9800f4e09", result.requestId)
        assertEquals("45000", result.rewardsAmount)
        assertEquals("Points", result.rewardsUnit)
        assertNull(result.errorCode)
        assertNull(result.errorMessage)
    }

    @Test(timeout = 1000)
    fun fromJson_parsesErrorFields() {
        val json = JSONObject().apply {
            put("error", JSONObject().apply {
                put("code", "INQ2003")
                put("message", "Card is ineligible")
            })
        }.toString()

        val result = AmericanExpressRewardsBalance.fromJson(json)

        assertEquals("INQ2003", result.errorCode)
        assertEquals("Card is ineligible", result.errorMessage)
    }

    @Test(timeout = 1000)
    fun fromJson_parsesErrorWithRewardsFields() {
        val json = JSONObject().apply {
            put("error", JSONObject().apply {
                put("code", "INQ2002")
                put("message", "Insufficient points")
            })
            put("conversionRate", "0.01")
            put("currencyAmount", "50.00")
            put("currencyIsoCode", "USD")
            put("requestId", "req-123")
            put("rewardsAmount", "5000")
            put("rewardsUnit", "Points")
        }.toString()

        val result = AmericanExpressRewardsBalance.fromJson(json)

        assertEquals("INQ2002", result.errorCode)
        assertEquals("Insufficient points", result.errorMessage)
        assertEquals("0.01", result.conversionRate)
        assertEquals("50.00", result.currencyAmount)
    }

    @Test(timeout = 1000)
    fun fromJson_withEmptyJson_returnsNullFields() {
        val result = AmericanExpressRewardsBalance.fromJson("{}")

        assertNull(result.conversionRate)
        assertNull(result.currencyAmount)
        assertNull(result.currencyIsoCode)
        assertNull(result.requestId)
        assertNull(result.rewardsAmount)
        assertNull(result.rewardsUnit)
        assertNull(result.errorCode)
        assertNull(result.errorMessage)
    }

    @Test(timeout = 1000, expected = JSONException::class)
    fun fromJson_withMalformedJson_throwsJSONException() {
        AmericanExpressRewardsBalance.fromJson("not valid json")
    }

    @Test(timeout = 1000)
    fun parceling_preservesAllFields() {
        val original = AmericanExpressRewardsBalance(
            conversionRate = "0.0070",
            currencyAmount = "316.00",
            currencyIsoCode = "USD",
            requestId = "req-456",
            rewardsAmount = "45000",
            rewardsUnit = "Points",
            errorCode = null,
            errorMessage = null
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<AmericanExpressRewardsBalance>().createFromParcel(parcel)

        assertEquals(original.conversionRate, restored.conversionRate)
        assertEquals(original.currencyAmount, restored.currencyAmount)
        assertEquals(original.currencyIsoCode, restored.currencyIsoCode)
        assertEquals(original.requestId, restored.requestId)
        assertEquals(original.rewardsAmount, restored.rewardsAmount)
        assertEquals(original.rewardsUnit, restored.rewardsUnit)
        assertNull(restored.errorCode)
        assertNull(restored.errorMessage)

        parcel.recycle()
    }

    @Test(timeout = 1000)
    fun parceling_preservesErrorFields() {
        val original = AmericanExpressRewardsBalance(
            errorCode = "INQ2003",
            errorMessage = "Card is ineligible"
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = parcelableCreator<AmericanExpressRewardsBalance>().createFromParcel(parcel)

        assertEquals("INQ2003", restored.errorCode)
        assertEquals("Card is ineligible", restored.errorMessage)

        parcel.recycle()
    }
}