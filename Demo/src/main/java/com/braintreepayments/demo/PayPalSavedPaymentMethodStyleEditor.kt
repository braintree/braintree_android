package com.braintreepayments.demo

import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A bottom sheet with every merchant-configurable style value, so the component can be re-themed at runtime. Each
 * value starts at the SDK default when it's switched on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayPalSavedPaymentMethodStyleEditor(
    config: PayPalSavedPaymentMethodStyleConfig,
    onConfigChange: (PayPalSavedPaymentMethodStyleConfig) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onConfigChange(PayPalSavedPaymentMethodStyleConfig()) }) { Text("Reset") }
            Text(
                text = "Component Style",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text("Done") }
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("Visibility")
            SwitchRow("PayPal Logo", config.showPayPalLogo) { onConfigChange(config.copy(showPayPalLogo = it)) }
            SwitchRow("PayPal Label", config.showPayPalLabel) { onConfigChange(config.copy(showPayPalLabel = it)) }
            SwitchRow("Credit Messaging", config.showPayPalCreditMessaging) {
                onConfigChange(config.copy(showPayPalCreditMessaging = it))
            }

            SectionTitle("Appearance")
            OptionalColorRow("Background Color", WHITE, config.backgroundColor) {
                onConfigChange(config.copy(backgroundColor = it))
            }
            OptionalColorRow("Text Color", DARK_GRAY, config.textColor) {
                onConfigChange(config.copy(textColor = it))
            }
            OptionalMetricRow("Base Font Size", "sp", defaultValue = 14f, value = config.baseFontSizeSp) {
                onConfigChange(config.copy(baseFontSizeSp = it))
            }

            SectionTitle("Container")
            OptionalMetricRow("Height", "dp", defaultValue = 48f, range = 0f..200f, value = config.heightDp) {
                onConfigChange(config.copy(heightDp = it))
            }
            OptionalMetricRow("Horizontal Padding", "dp", defaultValue = 0f, value = config.horizontalPaddingDp) {
                onConfigChange(config.copy(horizontalPaddingDp = it))
            }
            OptionalMetricRow("Vertical Padding", "dp", defaultValue = 10f, value = config.verticalPaddingDp) {
                onConfigChange(config.copy(verticalPaddingDp = it))
            }
            OptionalMetricRow(
                "Corner Radius", "dp", defaultValue = 0f, range = 0f..40f, value = config.cornerRadiusDp
            ) {
                onConfigChange(config.copy(cornerRadiusDp = it))
            }
            OptionalColorRow("Border Color", GRAY, config.borderColor) {
                onConfigChange(config.copy(borderColor = it))
            }
            OptionalMetricRow("Border Width", "dp", defaultValue = 1f, range = 0f..10f, value = config.borderWidthDp) {
                onConfigChange(config.copy(borderWidthDp = it))
            }

            SectionTitle("Logo and Label")
            OptionalMetricRow("Logo Width", "dp", defaultValue = 48f, range = 0f..120f, value = config.logoWidthDp) {
                onConfigChange(config.copy(logoWidthDp = it))
            }
            OptionalMetricRow("Label Font Size", "sp", defaultValue = 20f, value = config.labelFontSizeSp) {
                onConfigChange(config.copy(labelFontSizeSp = it))
            }
            OptionalMetricRow("Label Start Margin", "dp", defaultValue = 6f, value = config.labelMarginStartDp) {
                onConfigChange(config.copy(labelMarginStartDp = it))
            }

            SectionTitle("Funding Instrument")
            OptionalMetricRow(
                "Text Font Size", "sp", defaultValue = 14f, value = config.fundingInstrumentTextFontSizeSp
            ) {
                onConfigChange(config.copy(fundingInstrumentTextFontSizeSp = it))
            }
            OptionalMetricRow("Edit Icon Size", "dp", defaultValue = 16f, value = config.editIconSizeDp) {
                onConfigChange(config.copy(editIconSizeDp = it))
            }
            OptionalMetricRow("Start Margin", "dp", defaultValue = 12f, value = config.fundingInstrumentMarginStartDp) {
                onConfigChange(config.copy(fundingInstrumentMarginStartDp = it))
            }

            SectionTitle("Credit Messaging")
            OptionalMetricRow("Font Size", "sp", defaultValue = 16f, value = config.creditMessagingFontSizeSp) {
                onConfigChange(config.copy(creditMessagingFontSizeSp = it))
            }
            OptionalColorRow("Link Color", BLUE, config.creditMessagingLinkColor) {
                onConfigChange(config.copy(creditMessagingLinkColor = it))
            }
        }
    }
}

@Composable
internal fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.size(8.dp))
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

/** A size that may be left unset. Off hands `null` to the SDK; on shows a slider. */
@Composable
private fun OptionalMetricRow(
    title: String,
    unit: String,
    defaultValue: Float,
    range: ClosedFloatingPointRange<Float> = 0f..DEFAULT_MAX_VALUE,
    value: Float?,
    onValueChange: (Float?) -> Unit
) {
    Column {
        SwitchRow(title, value != null) { onValueChange(if (it) defaultValue else null) }
        if (value != null) {
            Text("${value.roundToInt()} $unit", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = value,
                onValueChange = { onValueChange(it.roundToInt().toFloat()) },
                valueRange = range
            )
        }
    }
}

/** A color that may be left unset. Off hands `null` to the SDK; on shows a few colors to pick from. */
@Composable
private fun OptionalColorRow(
    title: String,
    @ColorInt defaultColor: Int,
    @ColorInt value: Int?,
    onValueChange: (Int?) -> Unit
) {
    Column {
        SwitchRow(title, value != null) { onValueChange(if (it) defaultColor else null) }
        if (value != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (listOf(defaultColor) + COLOR_CHOICES).distinct().forEach { color ->
                    val outline = if (color == value) MaterialTheme.colorScheme.primary else Color.LightGray
                    Spacer(
                        Modifier
                            .size(32.dp)
                            .background(Color(color), CircleShape)
                            .border(width = if (color == value) 3.dp else 1.dp, color = outline, shape = CircleShape)
                            .clickable { onValueChange(color) }
                    )
                }
            }
        }
    }
}

private const val DEFAULT_MAX_VALUE = 60f
private const val WHITE = 0xFFFFFFFF.toInt()
private const val DARK_GRAY = 0xFF222222.toInt()
private const val GRAY = 0xFF8E8E93.toInt()
private const val BLUE = 0xFF0070E0.toInt()
private val COLOR_CHOICES = listOf(
    WHITE,
    0xFFF5F7FA.toInt(),
    DARK_GRAY,
    GRAY,
    BLUE,
    0xFF003087.toInt(),
    0xFFD20000.toInt()
)
