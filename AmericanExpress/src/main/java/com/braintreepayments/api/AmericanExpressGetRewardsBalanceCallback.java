package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.AmericanExpressRewardsBalance;

public interface AmericanExpressGetRewardsBalanceCallback {
    void onResult(@Nullable AmericanExpressRewardsBalance rewardsBalance, @Nullable Exception error);
}
