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
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.uicomponents.UIComponentsAnalytics
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResult
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsResultCallback
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel
import kotlinx.coroutines.flow.StateFlow

class CardFieldsController internal constructor(
    internal val viewModel: CardFieldsViewModel,
    internal val cardNumber: MutableState<TextFieldValue>,
    internal val expiration: MutableState<TextFieldValue>,
    internal val cvv: MutableState<TextFieldValue>,
    private val cardClient: CardClient,
    private val request: Card = Card(),
    private val analyticsClient: AnalyticsClient = AnalyticsClient.lazyInstance.value,
    private val shouldSendPresentedEvent: MutableState<Boolean> = mutableStateOf(true),
) {
    val isFormValid: StateFlow<Boolean> = viewModel.isFormValid

    init {
        if (shouldSendPresentedEvent.value) {
            analyticsClient.sendEvent(
                UIComponentsAnalytics.CARD_FIELDS_PRESENTED,
                AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
            )
            shouldSendPresentedEvent.value = false
        }
    }

    /**
     * Tokenizes the card details entered by the user, merged with any additional data provided via
     * [request]. The result is delivered to [callback].
     */
    fun submit(callback: CardFieldsResultCallback) {
        analyticsClient.sendEvent(
            UIComponentsAnalytics.CARD_FIELDS_VALIDATED,
            AnalyticsEventParams(uiType = UIComponentsAnalytics.UI_TYPE_COMPOSE)
        )

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
    val shouldSendPresentedEvent = rememberSaveable { mutableStateOf(true) }

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
        CardFieldsController(
            viewModel,
            cardNumber,
            expiration,
            cvv,
            CardClient(context, authorization),
            request,
            shouldSendPresentedEvent = shouldSendPresentedEvent,
        )
    }
}
