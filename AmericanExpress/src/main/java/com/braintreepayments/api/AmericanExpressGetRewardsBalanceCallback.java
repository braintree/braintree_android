package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Interface that defines callbacks for {@link com.braintreepayments.api.AmericanExpressClient}.
 */
public interface AmericanExpressGetRewardsBalanceCallback {

    /**
     * Will be called when
     * {@link AmericanExpressRewardsBalance} has been successfully fetched.
     */
    void onResult(@Nullable AmericanExpressRewardsBalance rewardsBalance, @Nullable Exception error);
}
