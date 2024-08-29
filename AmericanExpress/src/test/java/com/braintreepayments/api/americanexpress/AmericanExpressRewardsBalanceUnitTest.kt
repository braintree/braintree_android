package com.braintreepayments.api.americanexpress

import com.braintreepayments.api.americanexpress.AmericanExpressRewardsBalance.Companion.fromJson
import com.braintreepayments.api.testutils.Fixtures
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AmericanExpressRewardsBalanceUnitTest {
    @Test
    @Throws(JSONException::class)
    fun fromJson_parsesResponse() {
        val rewardsBalance = fromJson(Fixtures.AMEX_REWARDS_BALANCE_SUCCESS)

        assertEquals("0.0070", rewardsBalance.conversionRate)
        assertEquals("316795.03", rewardsBalance.currencyAmount)
        assertEquals("USD", rewardsBalance.currencyIsoCode)
        assertEquals("715f4712-8690-49ed-8cc5-d7fb1c2d", rewardsBalance.requestId)
        assertEquals("45256433", rewardsBalance.rewardsAmount)
        assertEquals("Points", rewardsBalance.rewardsUnit)
        assertNull(rewardsBalance.errorMessage)
        assertNull(rewardsBalance.errorCode)
    }

    @Test
    @Throws(JSONException::class)
    fun fromJson_parsesResponseForIneligibleCard() {
        val rewardsBalance = fromJson(Fixtures.AMEX_REWARDS_BALANCE_INELIGIBLE_CARD)

        assertNull(rewardsBalance.conversionRate)
        assertNull(rewardsBalance.currencyAmount)
        assertNull(rewardsBalance.currencyIsoCode)
        assertNull(rewardsBalance.requestId)
        assertNull(rewardsBalance.rewardsAmount)
        assertNull(rewardsBalance.rewardsUnit)
        assertEquals("Card is ineligible", rewardsBalance.errorMessage)
        assertEquals("INQ2002", rewardsBalance.errorCode)
    }

    @Test
    @Throws(JSONException::class)
    fun parcelsCorrectly() {
        val rewardsBalance = fromJson(Fixtures.AMEX_REWARDS_BALANCE_SUCCESS)

        assertEquals("0.0070", rewardsBalance.conversionRate)
        assertEquals("316795.03", rewardsBalance.currencyAmount)
        assertEquals("USD", rewardsBalance.currencyIsoCode)
        assertEquals("715f4712-8690-49ed-8cc5-d7fb1c2d", rewardsBalance.requestId)
        assertEquals("45256433", rewardsBalance.rewardsAmount)
        assertEquals("Points", rewardsBalance.rewardsUnit)
        assertNull(rewardsBalance.errorMessage)
        assertNull(rewardsBalance.errorCode)
    }

    @Test
    @Throws(JSONException::class)
    fun parcelsCorrectly_forErrorResponse() {
        val rewardsBalance = fromJson(Fixtures.AMEX_REWARDS_BALANCE_INSUFFICIENT_POINTS)

        assertNull(rewardsBalance.conversionRate)
        assertNull(rewardsBalance.currencyAmount)
        assertNull(rewardsBalance.currencyIsoCode)
        assertNull(rewardsBalance.requestId)
        assertNull(rewardsBalance.rewardsAmount)
        assertNull(rewardsBalance.rewardsUnit)
        assertEquals("Insufficient points on card", rewardsBalance.errorMessage)
        assertEquals("INQ2003", rewardsBalance.errorCode)
    }
}
