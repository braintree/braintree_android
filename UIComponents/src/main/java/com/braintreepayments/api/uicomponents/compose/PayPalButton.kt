package com.braintreepayments.api.uicomponents.compose

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.widget.Toast
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.uicomponents.PayPalButtonColor
import com.braintreepayments.api.uicomponents.R

@Composable
fun PayPalButtonCompose(color: PayPalButtonColor, loading: Boolean = false, onClick: () -> Unit) {
    val gradientDrawable = GradientDrawable()
    val focusIndicatorDrawable = GradientDrawable()
    var logo: Drawable? = null

    val ppLogoOffset = dimensionResource(R.dimen.pp_logo_offset)
    var logoOffset = ppLogoOffset
    val desiredWidth = dimensionResource(R.dimen.pay_button_width)
    val desiredHeight = dimensionResource(R.dimen.pay_button_height)
    val minDesiredWidth = dimensionResource(R.dimen.pay_button_min_width)
    val color: Color = when (color) {
        PayPalButtonColor.Blue -> {
            Color.Blue
        }
        PayPalButtonColor.Black -> {
            Color.Black
        }
        else -> {
            Color.White
        }
    }
    Button(
        onClick = { onClick() },
        modifier = Modifier
            .height(desiredHeight)
            .width(desiredWidth)
            .defaultMinSize(minWidth = minDesiredWidth),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        if (!loading) {
            Text(text = "PayPal")
        } else {
            CircularProgressIndicator(
                modifier = Modifier.width(24.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Preview
@Composable
fun Preview() {
    PayPalButtonCompose(color = PayPalButtonColor.Black, loading = false) {
        Toast.makeText(LocalContext.current, "Clicked", Toast.LENGTH_SHORT).show()
    }
}
