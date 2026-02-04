package com.braintreepayments.api.uicomponents.compose

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.widget.Toast
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@Composable
fun PayPalButton(style: PayPalButtonColor, enabled: Boolean = true, onClick: () -> Unit) {
    val context = LocalContext.current
    val logo: Drawable? = ContextCompat.getDrawable(context, style.logoId)

    val ppLogoOffset = dimensionResource(R.dimen.pp_logo_offset)
    val desiredWidth = dimensionResource(R.dimen.pay_button_width)
    val desiredHeight = dimensionResource(R.dimen.pay_button_height)
    val minDesiredWidth = dimensionResource(R.dimen.pay_button_min_width)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState()
    val isHovered = interactionSource.collectIsHoveredAsState()
    val isFocused = interactionSource.collectIsFocusedAsState()

    val containerColor = when {
        isPressed.value -> style.pressed.fill
        isHovered.value && isFocused.value -> style.focusHover.fill
        isHovered.value -> style.hover.fill
        isFocused.value -> style.focus.fill
        else -> style.default.fill
    }

    val borderColor = when {
        isPressed.value -> style.pressed.border
        isHovered.value && isFocused.value -> style.focusHover.border
        isHovered.value -> style.hover.border
        isFocused.value -> style.focus.border
        else -> style.default.border
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.semantics { role = Role.Button },
        enabled = enabled,
        shape = ButtonDefaults.shape,
        color = Color(ContextCompat.getColor(context, containerColor)),
        border = BorderStroke(2.dp, Color(ContextCompat.getColor(context, borderColor))),
        interactionSource = interactionSource
    ) {
        Row(
            Modifier
                .defaultMinSize(
                    minWidth = minDesiredWidth
                )
                .width(desiredWidth)
                .height(desiredHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = {
                if (enabled) {
                    Image(
                        painter = rememberDrawablePainter(drawable = logo),
                        modifier = Modifier.padding(top = ppLogoOffset),
                        contentDescription = "PayPal",
                    )
                } else {
                    val image = AnimatedImageVector.animatedVectorResource(style.spinnerId)
                    var atEnd by remember { mutableStateOf(true) }
                    Image(
                        painter = rememberAnimatedVectorPainter(image, atEnd),
                        contentDescription = "",
                    )
                }
            }
        )
    }
}

@Preview
@Composable
fun PreviewButtons() {
    val context = LocalContext.current
    Column {
        PayPalButton(style = PayPalButtonColor.Blue) {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        }
        PayPalButton(style = PayPalButtonColor.Black) {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        }
        PayPalButton(style = PayPalButtonColor.White) {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        }

        PayPalButton(style = PayPalButtonColor.Black, enabled = false) {
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show()
        }
    }
}
