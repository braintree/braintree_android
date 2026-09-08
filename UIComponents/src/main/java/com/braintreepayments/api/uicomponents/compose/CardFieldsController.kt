package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.card.Card
import com.braintreepayments.api.card.CardClient
import com.braintreepayments.api.card.CardResult
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResultCallback
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the UI state and tokenization logic for the [CardFields] composable. Create an instance
 * via [rememberCardFieldsController] rather than calling this constructor directly.
 * @param viewModel: The [CardFieldsViewModel] backing the card number, expiration, and CVV validation state.
 * @param cardNumber: The current card number field value.
 * @param expiration: The current expiration date field value.
 * @param cvv: The current CVV field value.
 * @param cardClient: The [CardClient] used to tokenize the card on [submit].
 * @param request: Additional card data to merge with the user-entered fields on [submit].
 */
class CardFieldsController internal constructor(
    internal val viewModel: CardFieldsViewModel,
    internal val cardNumber: MutableState<TextFieldValue>,
    internal val expiration: MutableState<TextFieldValue>,
    internal val cvv: MutableState<TextFieldValue>,
    private val cardClient: CardClient,
    private val request: Card = Card(),
) {
    val isFormValid: StateFlow<Boolean> = viewModel.isFormValid

    /**
     * Tokenizes the card details entered by the user, merged with any additional data provided via
     * [request]. The result is delivered to [callback].
     * @param callback: A [CardFieldsResultCallback] that handles the result of the tokenization.
     */
    fun submit(callback: CardFieldsResultCallback) {
        cardClient.tokenize(buildCard()) { cardResult ->
            val result = when (cardResult) {
                is CardResult.Success -> CardFieldsResult.Success(cardResult.nonce)
                is CardResult.Failure -> CardFieldsResult.Failure(cardResult.error)
            }
            callback.onCardFieldsResult(result)
        }
    }

    private fun buildCard(): Card {
        val rawExpiration = expiration.value.text
        return request.copy(
            number = cardNumber.value.text,
            expirationMonth = rawExpiration.take(2),
            expirationYear = rawExpiration.drop(2),
            cvv = cvv.value.text
        )
    }
}

private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

/**
 * Creates and remembers a [CardFieldsController] for use with the [CardFields] composable,
 * surviving recomposition, configuration changes, and process death.
 * @param authorization: A Braintree tokenization key or client token.
 * @param request: Additional card data (e.g. cardholder name, postal code) to merge with the
 * user-entered card number, expiration, and CVV when [CardFieldsController.submit] is called.
 */
@Composable
fun rememberCardFieldsController(authorization: String, request: Card = Card()): CardFieldsController {
    val context = LocalContext.current
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

    return remember(viewModel, authorization, request) {
        CardFieldsController(viewModel, cardNumber, expiration, cvv, CardClient(context, authorization), request)
    }
}
