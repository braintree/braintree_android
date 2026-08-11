package com.braintreepayments.api.uicomponents.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.braintreepayments.api.uicomponents.R

/**
 * Compose equivalent of [com.braintreepayments.api.uicomponents.cardfields.BaseTextInputView]:
 * a bordered container with an animated floating hint label, an error message, and optional
 * leading/trailing icon slots. Styling is pulled from the same dimens/colors the XML `CardFields`
 * view uses so both variants stay visually consistent.
 */
@Suppress("LongParameterList")
@Composable
internal fun CardFieldTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    focusRequester: FocusRequester? = null,
    onFocusChanged: (Boolean) -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val hasError = errorText != null

    val cornerRadius = dimensionResource(R.dimen.card_field_corner_radius)
    val borderWidth = dimensionResource(R.dimen.card_field_border_width)
    val borderFocusedWidth = dimensionResource(R.dimen.card_field_border_focused_width)
    val minHeight = dimensionResource(R.dimen.card_field_min_height)
    val paddingHorizontal = dimensionResource(R.dimen.card_field_padding_horizontal)
    val inputMarginBottom = dimensionResource(R.dimen.card_field_input_margin_bottom)
    val hintFloatTopMargin = dimensionResource(R.dimen.card_field_hint_float_top_margin)
    val hintRestTextSize = dimensionResource(R.dimen.card_field_hint_text_size).value.sp
    val hintFloatTextSize = dimensionResource(R.dimen.card_field_hint_float_text_size).value.sp

    val borderColor = colorResource(
        when {
            hasError -> R.color.card_field_error
            isFocused -> R.color.card_field_border_focused
            else -> R.color.card_field_border_default
        }
    )
    val currentBorderWidth = if (hasError || isFocused) borderFocusedWidth else borderWidth

    val shouldFloat = isFocused || value.text.isNotEmpty()
    val floatFraction by animateFloatAsState(
        targetValue = if (shouldFloat) 1f else 0f,
        animationSpec = tween(HINT_ANIMATION_DURATION_MS),
        label = "cardFieldHintFloat"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(colorResource(R.color.card_field_background))
                .border(currentBorderWidth, borderColor, RoundedCornerShape(cornerRadius))
                .padding(horizontal = paddingHorizontal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()

            var containerHeightPx by remember { mutableIntStateOf(0) }
            var hintHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = minHeight)
                    .onGloballyPositioned { containerHeightPx = it.size.height }
            ) {
                val translationYPx = with(density) {
                    val centerY = (containerHeightPx - hintHeightPx) / 2f
                    val floatTargetPx = hintFloatTopMargin.toPx() - centerY
                    floatFraction * floatTargetPx
                }

                Text(
                    text = hint,
                    fontSize = interpolateHintFontSize(hintRestTextSize, hintFloatTextSize, floatFraction),
                    color = colorResource(R.color.card_field_hint_text),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .onGloballyPositioned { hintHeightPx = it.size.height }
                        .graphicsLayer { translationY = translationYPx }
                )

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(bottom = inputMarginBottom)
                        .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                        .onFocusChanged {
                            isFocused = it.isFocused
                            onFocusChanged(it.isFocused)
                        }
                        .semantics { if (errorText != null) error(errorText) },
                    textStyle = TextStyle(
                        color = colorResource(R.color.card_field_text),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Start
                    ),
                    singleLine = true,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    interactionSource = interactionSource
                )
            }

            trailingIcon?.invoke()
        }

        if (errorText != null) {
            Text(
                text = errorText,
                color = colorResource(R.color.card_field_text),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.card_field_error_margin_top))
            )
        }
    }
}

private fun interpolateHintFontSize(restSize: TextUnit, floatedSize: TextUnit, fraction: Float) =
    (restSize.value + (floatedSize.value - restSize.value) * fraction).sp

private const val HINT_ANIMATION_DURATION_MS = 200
