package com.braintreepayments.api.uicomponents.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.braintreepayments.api.uicomponents.R

@Composable
internal fun CardExpirationField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    CardFieldBaseTextInputField(
        value = value,
        onValueChange = onValueChange,
        hint = stringResource(R.string.expiration_hint),
        modifier = modifier,
        errorText = errorText,
        visualTransformation = ExpirationDateVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        focusRequester = focusRequester,
        onFocusChanged = onFocusChanged,
        contentDescription = stringResource(R.string.expiration_hint_accessibility)
    )
}
