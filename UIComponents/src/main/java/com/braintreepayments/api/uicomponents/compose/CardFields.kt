package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import com.braintreepayments.api.uicomponents.cardfields.CardBrand
import com.braintreepayments.api.uicomponents.cardfields.CardField
import com.braintreepayments.api.uicomponents.cardfields.ValidationResult

@Composable
fun CardFields(controller: CardFieldsController, modifier: Modifier = Modifier) {
    val viewModel = controller.viewModel
    val cardNumberValidation by viewModel.cardNumberValidation.collectAsState()
    val detectedBrand by viewModel.detectedCardBrand.collectAsState()

    val focusManager = LocalFocusManager.current

    var cardNumberHasBeenFocused by remember { mutableStateOf(false) }

    fun onFieldFocusChanged(field: CardField, focused: Boolean, hasBeenFocused: Boolean): Boolean {
        if (focused || hasBeenFocused) {
            viewModel.onFieldFocusChanged(field, focused)
        }
        return focused || hasBeenFocused
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
            }
    ) {
        CardNumberField(
            value = controller.cardNumber.value,
            onValueChange = { newValue ->
                val sanitized = sanitizeCardNumberInput(newValue) ?: return@CardNumberField
                controller.cardNumber.value = sanitized
                viewModel.onCardNumberChanged(sanitized.text)
            },
            brand = detectedBrand,
            errorText = cardNumberValidation.errorText(),
            onFocusChanged = { focused ->
                cardNumberHasBeenFocused = onFieldFocusChanged(
                    CardField.CARD_NUMBER,
                    focused,
                    cardNumberHasBeenFocused
                )
            }
        )
    }
}

@Composable
private fun ValidationResult.errorText(): String? =
    (this as? ValidationResult.Invalid)?.let { stringResource(it.errorMessageRes) }

/**
 * Strips non-digit characters from [newValue] and rejects the edit entirely (returns `null`)
 * if it would push the digit count past the detected brand's max length. Rejecting outright —
 * rather than truncating — avoids the field appearing to "overwrite" digits at the end when the
 * user inserts new digits in the middle of an already-full number.
 */
internal fun sanitizeCardNumberInput(newValue: TextFieldValue): TextFieldValue? {
    val rawDigits = newValue.text.filter { it.isDigit() }
    val maxLength = CardBrand.resolveBrand(rawDigits).maxLength
    if (rawDigits.length > maxLength) return null
    return newValue.copy(text = rawDigits)
}
