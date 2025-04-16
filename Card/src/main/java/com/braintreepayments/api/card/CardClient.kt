package com.braintreepayments.api.card

import android.content.Context
import com.braintreepayments.api.card.CardNonce.Companion.fromJSON
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONException
import org.json.JSONObject

/**
 * Used to tokenize credit or debit cards using a [Card]. For more information see the
 * [documentation](https://developer.paypal.com/braintree/docs/guides/credit-cards/overview)
 */
class CardClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val apiClient: ApiClient = ApiClient(braintreeClient),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance
) {

    /**
     * Initializes a new [CardClient] instance
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
     * Create a [CardNonce].
     *
     *
     * The tokenization result is returned via a [CardTokenizeCallback] callback.
     *
     *
     *
     * On success, the [CardTokenizeCallback.onCardResult] method will be
     * invoked with a [CardResult.Success] including a nonce.
     *
     *
     *
     * If creation fails validation, the [CardTokenizeCallback.onCardResult]
     * method will be invoked with a [CardResult.Failure] including an
     * [ErrorWithResponse] exception.
     *
     *
     *
     * If an error not due to validation (server error, network issue, etc.) occurs, the
     * [CardTokenizeCallback.onCardResult] method will be invoked with a
     * [CardResult.Failure] with an [Exception] describing the error.
     *
     * @param card     [Card]
     * @param callback [CardTokenizeCallback]
     */
    fun tokenize(card: Card, callback: CardTokenizeCallback) {
        analyticsParamRepository.reset()
        braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_STARTED)
        braintreeClient.getConfiguration { configuration: Configuration?, error: Exception? ->
            if (error != null) {
                callbackFailure(callback, CardResult.Failure(error))
                return@getConfiguration
            }
            val shouldTokenizeViaGraphQL =
                configuration?.isGraphQLFeatureEnabled(
                    GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS
                ) ?: run {
                    false
                }
            if (shouldTokenizeViaGraphQL) {
                card.sessionId = analyticsParamRepository.sessionId
                try {
                    val tokenizePayload = card.buildJSONForGraphQL()
                    apiClient.tokenizeGraphQL(
                        tokenizePayload
                    ) { tokenizationResponse: JSONObject?, exception: Exception? ->
                        handleTokenizeResponse(
                            tokenizationResponse, exception, callback
                        )
                    }
                } catch (e: BraintreeException) {
                    callbackFailure(callback, CardResult.Failure(e))
                } catch (e: JSONException) {
                    callbackFailure(callback, CardResult.Failure(e))
                }
            } else {
                apiClient.tokenizeREST(
                    card
                ) { tokenizationResponse: JSONObject?, exception: Exception? ->
                    handleTokenizeResponse(
                        tokenizationResponse, exception, callback
                    )
                }
            }
        }
    }

    private fun handleTokenizeResponse(
        tokenizationResponse: JSONObject?, exception: Exception?,
        callback: CardTokenizeCallback
    ) {
        if (tokenizationResponse != null) {
            try {
                val cardNonce = fromJSON(tokenizationResponse)
                callbackSuccess(callback, CardResult.Success(cardNonce))
            } catch (e: JSONException) {
                callbackFailure(callback, CardResult.Failure(e))
            }
        } else if (exception != null) {
            callbackFailure(callback, CardResult.Failure(exception))
        }
    }

    private fun callbackFailure(callback: CardTokenizeCallback, cardResult: CardResult.Failure) {
        braintreeClient.sendAnalyticsEvent(
            CardAnalytics.CARD_TOKENIZE_FAILED,
            AnalyticsEventParams(errorDescription = cardResult.error.message)
        )
        callback.onCardResult(cardResult)
    }

    private fun callbackSuccess(callback: CardTokenizeCallback, cardResult: CardResult.Success) {
        braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_SUCCEEDED)
        callback.onCardResult(cardResult)
    }
}
