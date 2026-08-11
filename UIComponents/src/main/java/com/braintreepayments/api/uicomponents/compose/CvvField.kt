package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.cardfields.CardBrand
import kotlinx.coroutines.delay

/** Compose equivalent of [com.braintreepayments.api.uicomponents.cardfields.CvvTextInputView]. */
@Composable
internal fun CvvField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    brand: CardBrand,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    var showHint by remember { mutableStateOf(false) }
    var previousLength by remember { mutableIntStateOf(value.text.length) }
    var revealedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(value.text) {
        val grew = value.text.length > previousLength
        previousLength = value.text.length
        if (grew) {
            revealedIndex = value.text.lastIndex
            delay(CVV_DIGIT_REVEAL_DURATION_MS)
        }
        revealedIndex = null
    }

    CardFieldTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.text.length <= brand.cvvLength) onValueChange(newValue)
        },
        hint = stringResource(R.string.cvv_hint),
        modifier = modifier,
        errorText = errorText,
        visualTransformation = CvvVisualTransformation(revealedIndex),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        focusRequester = focusRequester,
        onFocusChanged = onFocusChanged,
        trailingIcon = {
            Box {
                IconButton(onClick = { showHint = true }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.cvv_hint),
                        contentDescription = stringResource(R.string.cvv_hint_icon_description)
                    )
                }
                if (showHint) {
                    CvvHintPopup(onDismissRequest = { showHint = false })
                }
            }
        }
    )
}

// Matches the ~1.5s reveal-then-mask timing of Android's platform PasswordTransformationMethod,
// which the XML CvvTextInputView gets for free via TYPE_NUMBER_VARIATION_PASSWORD.
private const val CVV_DIGIT_REVEAL_DURATION_MS = 1500L
