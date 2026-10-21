package com.braintreepayments.api.uicomponents.compose

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.braintreepayments.api.uicomponents.PayLaterButtonColor
import com.braintreepayments.api.uicomponents.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun PayLaterButtonView(style: PayLaterButtonColor, enabled: Boolean = true, onClick: () -> Unit) {
    val context = LocalContext.current
    val ppLogoOffset = dimensionResource(R.dimen.pp_logo_offset)
    val desiredWidth = dimensionResource(R.dimen.pay_button_width)
    val desiredHeight = dimensionResource(R.dimen.pay_button_height)
    val minDesiredWidth = dimensionResource(R.dimen.pay_button_min_width)
    val borderStroke = dimensionResource(R.dimen.pay_button_border)
    val focusBorderWidth = dimensionResource(R.dimen.pay_button_focus_border)
    val focusBorderPadding = dimensionResource(R.dimen.pay_button_focus_padding)
    val buttonCornerRadius = dimensionResource(R.dimen.pay_button_corner_radius)
    val buttonContentDescription = stringResource(R.string.pay_later_button_content_description)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val isHovered = interactionSource.collectIsHoveredAsState()
    val isFocused = interactionSource.collectIsFocusedAsState()

    val containerColor = colorResource(fillColor(style, isPressed.value, isHovered.value, isFocused.value))
    val borderColor = colorResource(borderColor(style, isPressed.value, isHovered.value, isFocused.value))
    val focusColor = colorResource(focusColor(style, isPressed.value, isHovered.value, isFocused.value))

    Surface(
        onClick = onClick,
        modifier = Modifier
            .semantics {
                role = Role.Button
                contentDescription = buttonContentDescription
            }
            .drawBehind {
                drawRoundRect(
                    focusColor,
                    cornerRadius = CornerRadius(buttonCornerRadius.toPx()),
                    style = Stroke(width = focusBorderWidth.toPx())
                )
            }
            .padding(focusBorderPadding),
        enabled = enabled,
        shape = RoundedCornerShape(buttonCornerRadius),
        color = containerColor,
        border = BorderStroke(borderStroke, borderColor),
        interactionSource = interactionSource
    ) {
        Row(
            Modifier
                .defaultMinSize(minWidth = minDesiredWidth)
                .width(desiredWidth)
                .height(desiredHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = {
                val loading = !enabled
                if (!loading) {
                    val logo: Drawable? = ContextCompat.getDrawable(context, style.logoId)
                    Image(
                        painter = rememberDrawablePainter(drawable = logo),
                        modifier = Modifier.padding(top = ppLogoOffset),
                        // The description lives on the button so it is announced once, for the whole control.
                        contentDescription = null,
                    )
                } else {
                    val color = when (style) {
                        PayLaterButtonColor.Blue -> Color.Black
                        PayLaterButtonColor.Black -> Color.White
                        PayLaterButtonColor.White -> Color.Black
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.size(desiredHeight / 2),
                        color = color,
                        strokeWidth = 2.dp
                    )
                }
            }
        )
    }
}

private fun fillColor(style: PayLaterButtonColor, isPressed: Boolean, isHovered: Boolean, isFocused: Boolean) = when {
    isPressed -> style.pressed.fill
    isHovered && isFocused -> style.focusHover.fill
    isHovered -> style.hover.fill
    isFocused -> style.focus.fill
    else -> style.default.fill
}

private fun borderColor(style: PayLaterButtonColor, isPressed: Boolean, isHovered: Boolean, isFocused: Boolean) = when {
    isPressed -> style.pressed.border
    isHovered && isFocused -> style.focusHover.border
    isHovered -> style.hover.border
    isFocused -> style.focus.border
    else -> style.default.border
}

private fun focusColor(style: PayLaterButtonColor, isPressed: Boolean, isHovered: Boolean, isFocused: Boolean) = when {
    isPressed -> style.pressed.focusIndicator
    isHovered && isFocused -> style.focusHover.focusIndicator
    isHovered -> style.hover.focusIndicator
    isFocused -> style.focus.focusIndicator
    else -> style.default.focusIndicator
}

@Preview(name = "Pay Later - Blue", showBackground = true)
@Composable
private fun PreviewPayLaterButtonBlue() {
    PayLaterButtonView(style = PayLaterButtonColor.Blue) {}
}

@Preview(name = "Pay Later - Black", showBackground = true)
@Composable
private fun PreviewPayLaterButtonBlack() {
    PayLaterButtonView(style = PayLaterButtonColor.Black) {}
}

@Preview(name = "Pay Later - White", showBackground = true)
@Composable
private fun PreviewPayLaterButtonWhite() {
    PayLaterButtonView(style = PayLaterButtonColor.White) {}
}

@Preview(name = "Pay Later - Loading", showBackground = true)
@Composable
private fun PreviewPayLaterButtonLoading() {
    PayLaterButtonView(style = PayLaterButtonColor.Blue, enabled = false) {}
}
