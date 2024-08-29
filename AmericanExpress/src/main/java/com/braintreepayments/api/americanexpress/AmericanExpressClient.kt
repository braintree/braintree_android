package com.braintreepayments.api.americanexpress

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.ApiClient.Companion.versionedPath
import com.braintreepayments.api.core.BraintreeClient
import org.json.JSONException

/**
 * Used to integrate with Braintree's American Express API
 */
class AmericanExpressClient(private val braintreeClient: BraintreeClient) {

    /**
     * Initializes a new [AmericanExpressClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        BraintreeClient(
            context,
            authorization
        )
    )

    /**
     * Gets the rewards balance associated with a Braintree nonce. Only for American Express cards.
     *
     * @param nonce   A nonce representing a card that will be used to look up the rewards balance
     * @param currencyIsoCode The currencyIsoCode to use. Example: 'USD'
     * @param callback        [AmericanExpressGetRewardsBalanceCallback]
     */
    fun getRewardsBalance(
        nonce: String,
        currencyIsoCode: String,
        callback: AmericanExpressGetRewardsBalanceCallback
    ) {
        val getRewardsBalanceUrl = Uri.parse(AMEX_REWARDS_BALANCE_PATH)
            .buildUpon()
            .appendQueryParameter("paymentMethodNonce", nonce)
            .appendQueryParameter("currencyIsoCode", currencyIsoCode)
            .build()
            .toString()

        braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED)
        braintreeClient.sendGET(getRewardsBalanceUrl) { responseBody: String?, httpError: Exception? ->
            if (responseBody != null) {
                try {
                    val rewardsBalance =
                        AmericanExpressRewardsBalance.fromJson(responseBody)
                    callbackSuccess(AmericanExpressResult.Success(rewardsBalance), callback)
                } catch (e: JSONException) {
                    callbackFailure(AmericanExpressResult.Failure(e), callback)
                }
            } else if (httpError != null) {
                callbackFailure(AmericanExpressResult.Failure(httpError), callback)
            }
        }
    }

    private fun callbackSuccess(
        result: AmericanExpressResult.Success,
        callback: AmericanExpressGetRewardsBalanceCallback
    ) {
        callback.onAmericanExpressResult(result)
        braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED)
    }

    private fun callbackFailure(
        result: AmericanExpressResult.Failure,
        callback: AmericanExpressGetRewardsBalanceCallback
    ) {
        callback.onAmericanExpressResult(result)
        braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_FAILED)
    }

    companion object {
        private val AMEX_REWARDS_BALANCE_PATH =
            versionedPath("payment_methods/amex_rewards_balance")
    }
}
