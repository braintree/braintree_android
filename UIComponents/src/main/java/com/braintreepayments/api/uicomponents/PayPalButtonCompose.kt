package com.braintreepayments.api.uicomponents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp

@Composable
fun PayPalButtonCompose(color: PayPalButtonColor, loading: Boolean = false, onClick: () -> Unit) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Button(
            onClick = { onClick() },
            modifier = Modifier
                .height(48.dp)
                .width(200.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color)
        ) {
            if (!loading) {
                Text(text = "Fancy PayPal Button")
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.width(24.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
