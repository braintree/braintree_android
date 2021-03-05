package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link AmericanExpressClient#getRewardsBalance(String, String, AmericanExpressGetRewardsBalanceCallback)}.
 */
public interface AmericanExpressGetRewardsBalanceCallback {

    /**
     * @param rewardsBalance {@link AmericanExpressRewardsBalance}
     * @param error an exception that occurred while fetching rewards balance
     */
    void onResult(@Nullable AmericanExpressRewardsBalance rewardsBalance, @Nullable Exception error);
}
