package com.braintreepayments.api.americanexpress

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.ApiClient.Companion.versionedPath
import com.braintreepayments.api.core.BraintreeClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Used to integrate with Braintree's American Express API
 */
class AmericanExpressClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(mainDispatcher),
) {

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
        coroutineScope.launch {
            val result = getRewardsBalance(nonce, currencyIsoCode)
            callback.onAmericanExpressResult(result)
        }
    }

    private suspend fun getRewardsBalance(
        nonce: String,
        currencyIsoCode: String
    ): AmericanExpressResult {
        val getRewardsBalanceUrl = Uri.parse(AMEX_REWARDS_BALANCE_PATH)
            .buildUpon()
            .appendQueryParameter("paymentMethodNonce", nonce)
            .appendQueryParameter("currencyIsoCode", currencyIsoCode)
            .build()
            .toString()

        braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED)
        try {
            val responseBody = braintreeClient.sendGET(getRewardsBalanceUrl)
            val rewardsBalance =
                AmericanExpressRewardsBalance.fromJson(responseBody)
            braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED)
            return AmericanExpressResult.Success(rewardsBalance)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return getRewardsBalanceFailure(AmericanExpressResult.Failure(e))
        }
    }

    private fun getRewardsBalanceFailure(
        result: AmericanExpressResult.Failure,
    ): AmericanExpressResult {
        braintreeClient.sendAnalyticsEvent(
            AmericanExpressAnalytics.REWARDS_BALANCE_FAILED,
            AnalyticsEventParams(errorDescription = result.error.message)
        )
        return result
    }

    companion object {
        private val AMEX_REWARDS_BALANCE_PATH =
            versionedPath("payment_methods/amex_rewards_balance")
    }
}
