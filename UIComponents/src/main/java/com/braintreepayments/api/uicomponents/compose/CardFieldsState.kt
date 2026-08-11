package com.braintreepayments.api.uicomponents.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResultCallback
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the state for the Compose [CardFields] composable and drives card tokenization. Obtain an
 * instance via [rememberCardFieldsState].
 */
class CardFieldsState internal constructor(
    internal val viewModel: CardFieldsViewModel,
    private var cardClient: CardClient? = null,
    private val analyticsClient: AnalyticsClient? = null
) {

    internal var cardNumber = mutableStateOf(viewModel.currentCardNumber.asTextFieldValue())
    internal var expiration = mutableStateOf(viewModel.currentExpiration.asTextFieldValue())
    internal var cvv = mutableStateOf(viewModel.currentCvv.asTextFieldValue())

    private var request: Card? = null

    private fun getAnalyticsClient(): AnalyticsClient = analyticsClient ?: AnalyticsClient.lazyInstance.value

    /** True once all three fields pass validation. */
    val isFormValid: StateFlow<Boolean> = viewModel.isFormValid

    /**
     * Initializes the card tokenization flow. Must be called before [submit].
     * @param authorization a tokenization key or client token.
     */
    fun initialize(context: Context, authorization: String) {
        cardClient = CardClient(context, authorization)
        getAnalyticsClient().sendEvent(
            UIComponentsAnalytics.CARD_FIELDS_PRESENTED,
            AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
        )
    }

    /**
     * Optionally accepts a [Card] with additional data to be included in the tokenization request, such as
     * cardholder name or billing address. The card number, expiration, and CVV entered by the user will
     * override any values set on the [Card] object.
     */
    fun setPaymentRequest(card: Card?) {
        request = card
    }

    /**
     * Tokenizes the card details entered by the user, along with any additional data provided in
     * [setPaymentRequest]. The result is delivered to [callback].
     *
     * If called before [initialize], delivers a [CardFieldsResult.Failure].
     */
    fun submit(callback: CardFieldsResultCallback) {
        val client = cardClient
        if (client == null) {
            callback.onCardFieldsResult(
                CardFieldsResult.Failure(
                    BraintreeException("CardFieldsState must be initialized by calling initialize() before submit()")
                )
            )
            return
        }
        getAnalyticsClient().sendEvent(
            UIComponentsAnalytics.CARD_FIELDS_VALIDATED,
            AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
        )
        client.tokenize(buildCard()) { cardResult ->
            val result = when (cardResult) {
                is CardResult.Success -> CardFieldsResult.Success(cardResult.nonce)
                is CardResult.Failure -> CardFieldsResult.Failure(cardResult.error)
            }
            callback.onCardFieldsResult(result)
        }
    }

    private fun buildCard(): Card {
        val rawExpiration = expiration.value.text
        return (request ?: Card()).copy(
            number = cardNumber.value.text,
            expirationMonth = rawExpiration.take(2),
            expirationYear = rawExpiration.drop(2),
            cvv = cvv.value.text
        )
    }
}

private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

/** Remembers a [CardFieldsState] backed by a view model that survives recomposition/rotation. */
@Composable
fun rememberCardFieldsState(): CardFieldsState {
    val viewModel = viewModel<CardFieldsViewModel>()
    return remember(viewModel) { CardFieldsState(viewModel) }
}
