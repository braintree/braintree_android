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
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun CardCvvField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    var showHint by remember { mutableStateOf(false) }
    var previousLength by remember { mutableIntStateOf(value.text.length) }
    var revealedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(value.text) {
        val hasNewDigit = value.text.length > previousLength
        previousLength = value.text.length
        val typedIndex = value.selection.end - 1
        if (hasNewDigit && typedIndex in value.text.indices) {
            revealedIndex = typedIndex
            delay(CVV_DIGIT_REVEAL_DURATION.milliseconds)
        }
        revealedIndex = null
    }

    CardFieldBaseTextInputField(
        value = value,
        onValueChange = onValueChange,
        hint = stringResource(R.string.cvv_hint),
        modifier = modifier,
        errorText = errorText,
        visualTransformation = remember(revealedIndex) { CvvVisualTransformation(revealedIndex) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        focusRequester = focusRequester,
        onFocusChanged = onFocusChanged,
        contentDescription = stringResource(R.string.cvv_accessibility),
        trailingIcon = {
            Box {
                IconButton(onClick = { showHint = true }) {
                    Icon(
                        painter = painterResource(R.drawable.cvv_hint),
                        contentDescription = stringResource(R.string.cvv_hint_icon_description),
                        modifier = Modifier.size(28.dp)
                    )
                }
                if (showHint) {
                    CvvHintPopup(onDismissRequest = { showHint = false })
                }
            }
        }
    )
}

private const val CVV_DIGIT_REVEAL_DURATION = 1500L
