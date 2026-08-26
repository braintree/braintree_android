package com.braintreepayments.api.uicomponents.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResultCallback
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import kotlinx.coroutines.flow.StateFlow

class CardFieldsController internal constructor(
    internal val viewModel: CardFieldsViewModel,
    internal val cardNumber: MutableState<TextFieldValue>,
    internal val expiration: MutableState<TextFieldValue>,
    internal val cvv: MutableState<TextFieldValue>,
    private var cardClient: CardClient? = null,
) {
    private var request: Card? = null

    val isFormValid: StateFlow<Boolean> = viewModel.isFormValid

    /**
     * Initializes the card tokenization flow. Must be called before [submit].
     * @param authorization a tokenization key or client token.
     */
    fun initialize(context: Context, authorization: String) {
        cardClient = CardClient(context, authorization)
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

@Composable
fun rememberCardFieldsController(): CardFieldsController {
    val viewModel = viewModel<CardFieldsViewModel>()
    val cardNumber = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(viewModel.currentCardNumber.asTextFieldValue())
    }
    val expiration = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(viewModel.currentExpiration.asTextFieldValue())
    }
    val cvv = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(viewModel.currentCvv.asTextFieldValue())
    }

    LaunchedEffect(viewModel) {
        if (cardNumber.value.text != viewModel.currentCardNumber) {
            viewModel.onCardNumberChanged(cardNumber.value.text)
        }
        if (expiration.value.text != viewModel.currentExpiration) {
            viewModel.onExpiryChanged(expiration.value.text)
        }
        if (cvv.value.text != viewModel.currentCvv) {
            viewModel.onCvvChanged(cvv.value.text)
        }
    }

    return remember(viewModel) { CardFieldsController(viewModel, cardNumber, expiration, cvv) }
}
