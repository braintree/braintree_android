package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.cardfields.CardBrand
import com.braintreepayments.api.uicomponents.cardfields.CardField
import com.braintreepayments.api.uicomponents.cardfields.ValidationResult

/**
 * A composable card entry form: card number, expiration, and CVV fields with live formatting,
 * brand detection, and blur-triggered validation. Obtain [state] via [rememberCardFieldsState].
 * Call [CardFieldsState.submit] from your own "Pay" button, enabled by observing
 * [CardFieldsState.isFormValid].
 */
@Composable
fun CardFields(state: CardFieldsState, modifier: Modifier = Modifier) {
    val viewModel = state.viewModel
    val cardNumberValidation by viewModel.cardNumberValidation.collectAsState()
    val expirationValidation by viewModel.expirationValidation.collectAsState()
    val cvvValidation by viewModel.cvvValidation.collectAsState()
    val detectedBrand by viewModel.detectedCardBrand.collectAsState()

    val focusManager = LocalFocusManager.current

    var cardNumberHasBeenFocused by remember { mutableStateOf(false) }
    var expirationHasBeenFocused by remember { mutableStateOf(false) }
    var cvvHasBeenFocused by remember { mutableStateOf(false) }

    fun onFieldFocusChanged(field: CardField, focused: Boolean, hasBeenFocused: Boolean): Boolean {
        if (focused || hasBeenFocused) {
            viewModel.onFieldFocusChanged(field, focused)
        }
        return focused || hasBeenFocused
    }

    Column(modifier = modifier) {
        CardNumberField(
            value = state.cardNumber.value,
            onValueChange = { newValue ->
                val rawDigits = newValue.text.filter { it.isDigit() }
                val digits = rawDigits.take(CardBrand.resolveBrand(rawDigits).maxLength)
                val wasValid = viewModel.cardNumberValidation.value is ValidationResult.Valid
                state.cardNumber.value = newValue.copy(text = digits)
                viewModel.onCardNumberChanged(digits)
                if (!wasValid && viewModel.cardNumberValidation.value is ValidationResult.Valid) {
                    focusManager.moveFocus(FocusDirection.Next)
                }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.card_field_field_spacing)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.card_field_field_spacing))
        ) {
            ExpirationField(
                value = state.expiration.value,
                onValueChange = { newValue ->
                    val digits = newValue.text.filter { it.isDigit() }.take(EXPIRATION_MAX_DIGITS)
                    val wasValid = viewModel.expirationValidation.value is ValidationResult.Valid
                    state.expiration.value = newValue.copy(text = digits)
                    viewModel.onExpiryChanged(digits)
                    if (!wasValid && viewModel.expirationValidation.value is ValidationResult.Valid) {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                },
                modifier = Modifier.weight(1f),
                errorText = expirationValidation.errorText(),
                onFocusChanged = { focused ->
                    expirationHasBeenFocused = onFieldFocusChanged(
                        CardField.EXPIRY,
                        focused,
                        expirationHasBeenFocused
                    )
                }
            )

            CvvField(
                value = state.cvv.value,
                onValueChange = { newValue ->
                    val digits = newValue.text.filter { it.isDigit() }
                    state.cvv.value = newValue.copy(text = digits)
                    viewModel.onCvvChanged(digits)
                },
                brand = detectedBrand,
                modifier = Modifier.weight(1f),
                errorText = cvvValidation.errorText(),
                onFocusChanged = { focused ->
                    cvvHasBeenFocused = onFieldFocusChanged(CardField.CVV, focused, cvvHasBeenFocused)
                }
            )
        }
    }
}

@Composable
private fun ValidationResult.errorText(): String? =
    (this as? ValidationResult.Invalid)?.let { stringResource(it.errorMessageRes) }

private const val EXPIRATION_MAX_DIGITS = 4
