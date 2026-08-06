package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.braintreepayments.api.uicomponents.R

/** Compose equivalent of [com.braintreepayments.api.uicomponents.cardfields.ExpirationTextInputView]. */
@Composable
internal fun ExpirationField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val accessibilityLabel = stringResource(R.string.expiration_hint_accessibility)
    CardFieldTextField(
        value = value,
        onValueChange = onValueChange,
        hint = stringResource(R.string.expiration_hint),
        modifier = modifier.semantics { contentDescription = accessibilityLabel },
        errorText = errorText,
        visualTransformation = ExpirationVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        focusRequester = focusRequester,
        onFocusChanged = onFocusChanged
    )
}
