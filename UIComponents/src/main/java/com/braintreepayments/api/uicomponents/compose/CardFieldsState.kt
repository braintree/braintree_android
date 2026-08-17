package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel

class CardFieldsState internal constructor(
    internal val viewModel: CardFieldsViewModel,
) {
    internal var cardNumber = mutableStateOf(viewModel.currentCardNumber.asTextFieldValue())
    internal var expiration = mutableStateOf(viewModel.currentExpiration.asTextFieldValue())
}

private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

@Composable
fun rememberCardFieldsState(): CardFieldsState {
    val viewModel = viewModel<CardFieldsViewModel>()
    return remember(viewModel) { CardFieldsState(viewModel) }
}
