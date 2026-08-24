package com.braintreepayments.api.uicomponents.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.braintreepayments.api.uicomponents.cardfields.CardFieldsViewModel

class CardFieldsController internal constructor(
    internal val viewModel: CardFieldsViewModel,
) {
    internal var cardNumber = mutableStateOf(viewModel.currentCardNumber.asTextFieldValue())
}

private fun String.asTextFieldValue() = TextFieldValue(text = this, selection = TextRange(length))

@Composable
fun rememberCardFieldsController(): CardFieldsController {
    val viewModel = viewModel<CardFieldsViewModel>()
    return remember(viewModel) { CardFieldsController(viewModel) }
}
