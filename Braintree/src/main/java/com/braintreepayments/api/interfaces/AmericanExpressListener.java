package com.braintreepayments.api.interfaces;

import com.braintreepayments.api.models.AmericanExpressRewardsBalance;

/**
 * Interface that defines callbacks for American Express.
 */
public interface AmericanExpressListener extends BraintreeListener {

    /**
     * Will be called when
     * {@link com.braintreepayments.api.models.AmericanExpressRewardsBalance} has been successfully fetched.
     */
    void onRewardsBalanceFetched(AmericanExpressRewardsBalance rewardsBalance);

}
