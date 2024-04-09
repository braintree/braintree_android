package com.braintreepayments.api.americanexpress;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import com.braintreepayments.api.ApiClient;
import com.braintreepayments.api.core.BraintreeClient;

import org.json.JSONException;

/**
 * Used to integrate with Braintree's American Express API
 */
public class AmericanExpressClient {

    private static final String AMEX_REWARDS_BALANCE_PATH =
            ApiClient.versionedPath("payment_methods/amex_rewards_balance");

    private final BraintreeClient braintreeClient;

    /**
     * Initializes a new {@link AmericanExpressClient} instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    public AmericanExpressClient(@NonNull Context context, @NonNull String authorization) {
        this.braintreeClient = new BraintreeClient(context, authorization);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public AmericanExpressClient(@NonNull BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Gets the rewards balance associated with a Braintree nonce. Only for American Express cards.
     *
     * @param nonce           A nonce representing a card that will be used to look up the rewards
     *                        balance
     * @param currencyIsoCode The currencyIsoCode to use. Example: 'USD'
     * @param callback        {@link AmericanExpressGetRewardsBalanceCallback}
     */
    public void getRewardsBalance(@NonNull String nonce, @NonNull String currencyIsoCode, @NonNull
    final AmericanExpressGetRewardsBalanceCallback callback) {
        String getRewardsBalanceUrl = Uri.parse(AMEX_REWARDS_BALANCE_PATH)
                .buildUpon()
                .appendQueryParameter("paymentMethodNonce", nonce)
                .appendQueryParameter("currencyIsoCode", currencyIsoCode)
                .build()
                .toString();

        braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED);
        braintreeClient.sendGET(getRewardsBalanceUrl, (responseBody, httpError) -> {
            if (responseBody != null) {
                try {
                    AmericanExpressRewardsBalance rewardsBalance =
                            AmericanExpressRewardsBalance.fromJson(responseBody);
                    callbackSuccess(new AmericanExpressResult.Success(rewardsBalance), callback);
                } catch (JSONException e) {
                    callbackFailure(new AmericanExpressResult.Failure(e), callback);
                }
            } else if (httpError != null) {
                callbackFailure(new AmericanExpressResult.Failure(httpError), callback);
            }
        });
    }

    private void callbackSuccess(AmericanExpressResult.Success result, AmericanExpressGetRewardsBalanceCallback callback) {
        callback.onAmericanExpressResult(result);
        braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED);
    }

    private void callbackFailure(AmericanExpressResult.Failure result, AmericanExpressGetRewardsBalanceCallback callback) {
        callback.onAmericanExpressResult(result);
        braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_FAILED);
    }
}
