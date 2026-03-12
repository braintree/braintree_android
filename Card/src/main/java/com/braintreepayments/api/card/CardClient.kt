package com.braintreepayments.api.card

import android.content.Context
import com.braintreepayments.api.card.CardNonce.Companion.fromJSON
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GraphQLConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Used to tokenize credit or debit cards using a [Card]. For more information see the
 * [documentation](https://developer.paypal.com/braintree/docs/guides/credit-cards/overview)
 */
class CardClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val apiClient: ApiClient = ApiClient(braintreeClient),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
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
     * The tokenization result is returned via a [CardTokenizeCallback] callback.
     *
     * On success, the [CardTokenizeCallback.onCardResult] method will be
     * invoked with a [CardResult.Success] including a nonce.
     *
     * If creation fails validation, the [CardTokenizeCallback.onCardResult]
     * method will be invoked with a [CardResult.Failure] including an exception.
     *
     * If an error not due to validation (server error, network issue, etc.) occurs, the
     * [CardTokenizeCallback.onCardResult] method will be invoked with a
     * [CardResult.Failure] with an [Exception] describing the error.
     *
     * @param card     [Card]
     * @param callback [CardTokenizeCallback]
     */
    fun tokenize(card: Card, callback: CardTokenizeCallback) {
        coroutineScope.launch {
            val result = tokenize(card)
            callback.onCardResult(result)
        }
    }

    private suspend fun tokenize(card: Card): CardResult {
        analyticsParamRepository.reset()
        braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_STARTED)
        return try {
            val configuration = braintreeClient.getConfiguration()
            val shouldTokenizeViaGraphQL =
                configuration.isGraphQLFeatureEnabled(
                    GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS
                )

            val tokenizationResponse = if (shouldTokenizeViaGraphQL) {
                card.sessionId = analyticsParamRepository.sessionId
                val tokenizePayload = card.buildJSONForGraphQL()
                apiClient.tokenizeGraphQL(tokenizePayload)
            } else {
                apiClient.tokenizeREST(card)
            }

            handleTokenizeResponse(tokenizationResponse)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            tokenizeFailure(e)
        }
    }

    private fun handleTokenizeResponse(tokenizationResponse: JSONObject): CardResult {
        return if (tokenizationResponse.has("errors") &&
            tokenizationResponse.getJSONArray(GraphQLConstants.Keys.ERRORS).length() > 0
        ) {
            tokenizeFailure(BraintreeException(tokenizationResponse.toString()))
        } else {
            try {
                val cardNonce = fromJSON(tokenizationResponse)
                braintreeClient.sendAnalyticsEvent(CardAnalytics.CARD_TOKENIZE_SUCCEEDED)
                CardResult.Success(cardNonce)
            } catch (e: JSONException) {
                tokenizeFailure(e)
            }
        }
    }

    private fun tokenizeFailure(error: Exception): CardResult.Failure {
        braintreeClient.sendAnalyticsEvent(
            CardAnalytics.CARD_TOKENIZE_FAILED,
            AnalyticsEventParams(errorDescription = error.message)
        )
        return CardResult.Failure(error)
    }
}
