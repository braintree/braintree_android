package com.braintreepayments.api;

import android.os.Parcel;

import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class AmericanExpressRewardsBalanceUnitTest {

    @Test
    public void fromJson_parsesResponse() throws JSONException {
        AmericanExpressRewardsBalance rewardsBalance = AmericanExpressRewardsBalance
                .fromJson(Fixtures.AMEX_REWARDS_BALANCE_SUCCESS);

        assertEquals("0.0070", rewardsBalance.getConversionRate());
        assertEquals("316795.03", rewardsBalance.getCurrencyAmount());
        assertEquals("USD", rewardsBalance.getCurrencyIsoCode());
        assertEquals("715f4712-8690-49ed-8cc5-d7fb1c2d", rewardsBalance.getRequestId());
        assertEquals("45256433", rewardsBalance.getRewardsAmount());
        assertEquals("Points", rewardsBalance.getRewardsUnit());
        assertNull(rewardsBalance.getErrorMessage());
        assertNull(rewardsBalance.getErrorCode());
    }

    @Test
    public void fromJson_parsesResponseForIneligibleCard() throws JSONException {
        AmericanExpressRewardsBalance rewardsBalance = AmericanExpressRewardsBalance
                .fromJson(Fixtures.AMEX_REWARDS_BALANCE_INELIGIBLE_CARD);

        assertNull(rewardsBalance.getConversionRate());
        assertNull(rewardsBalance.getCurrencyAmount());
        assertNull(rewardsBalance.getCurrencyIsoCode());
        assertNull(rewardsBalance.getRequestId());
        assertNull(rewardsBalance.getRewardsAmount());
        assertNull(rewardsBalance.getRewardsUnit());
        assertEquals("Card is ineligible", rewardsBalance.getErrorMessage());
        assertEquals("INQ2002", rewardsBalance.getErrorCode());
    }

    @Test
    public void parcelsCorrectly() throws JSONException {
        AmericanExpressRewardsBalance rewardsBalanceFromJson = AmericanExpressRewardsBalance
                .fromJson(Fixtures.AMEX_REWARDS_BALANCE_SUCCESS);

        Parcel parcel = Parcel.obtain();
        rewardsBalanceFromJson.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AmericanExpressRewardsBalance rewardsBalance = AmericanExpressRewardsBalance.CREATOR.createFromParcel(parcel);

        assertEquals("0.0070", rewardsBalance.getConversionRate());
        assertEquals("316795.03", rewardsBalance.getCurrencyAmount());
        assertEquals("USD", rewardsBalance.getCurrencyIsoCode());
        assertEquals("715f4712-8690-49ed-8cc5-d7fb1c2d", rewardsBalance.getRequestId());
        assertEquals("45256433", rewardsBalance.getRewardsAmount());
        assertEquals("Points", rewardsBalance.getRewardsUnit());
        assertNull(rewardsBalance.getErrorMessage());
        assertNull(rewardsBalance.getErrorCode());
    }

    @Test
    public void parcelsCorrectly_forErrorResponse() throws JSONException {
        AmericanExpressRewardsBalance rewardsBalanceFromJson = AmericanExpressRewardsBalance
                .fromJson(Fixtures.AMEX_REWARDS_BALANCE_INSUFFICIENT_POINTS);

        Parcel parcel = Parcel.obtain();
        rewardsBalanceFromJson.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AmericanExpressRewardsBalance rewardsBalance = AmericanExpressRewardsBalance.CREATOR.createFromParcel(parcel);

        assertNull(rewardsBalance.getConversionRate());
        assertNull(rewardsBalance.getCurrencyAmount());
        assertNull(rewardsBalance.getCurrencyIsoCode());
        assertNull(rewardsBalance.getRequestId());
        assertNull(rewardsBalance.getRewardsAmount());
        assertNull(rewardsBalance.getRewardsUnit());
        assertEquals("Insufficient points on card", rewardsBalance.getErrorMessage());
        assertEquals("INQ2003", rewardsBalance.getErrorCode());
    }
}
