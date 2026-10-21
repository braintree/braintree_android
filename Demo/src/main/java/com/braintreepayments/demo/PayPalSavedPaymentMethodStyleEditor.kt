package com.braintreepayments.demo

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
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
            TextButton(onClick = { onConfigChange(PayPalSavedPaymentMethodStyleConfig()) }) {
                Text(stringResource(R.string.paypal_saved_payment_method_style_reset))
            }
            Text(
                text = stringResource(R.string.paypal_saved_payment_method_style_component_style),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.paypal_saved_payment_method_style_done)) }
        }
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(R.string.paypal_saved_payment_method_style_visibility)
            SwitchRow(R.string.paypal_saved_payment_method_style_paypal_logo, config.showPayPalLogo) {
                onConfigChange(config.copy(showPayPalLogo = it))
            }
            SwitchRow(R.string.paypal_saved_payment_method_style_paypal_label, config.showPayPalLabel) {
                onConfigChange(config.copy(showPayPalLabel = it))
            }
            SwitchRow(R.string.paypal_saved_payment_method_style_credit_messaging, config.showPayPalCreditMessaging) {
                onConfigChange(config.copy(showPayPalCreditMessaging = it))
            }

            SectionTitle(R.string.paypal_saved_payment_method_style_appearance)
            OptionalColorRow(
                title = R.string.paypal_saved_payment_method_style_background_color,
                defaultColor = WHITE,
                value = config.backgroundColor
            ) { onConfigChange(config.copy(backgroundColor = it)) }
            OptionalColorRow(
                title = R.string.paypal_saved_payment_method_style_text_color,
                defaultColor = DARK_GRAY,
                value = config.textColor
            ) { onConfigChange(config.copy(textColor = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_base_font_size,
                unit = "sp",
                defaultValue = 14f,
                value = config.baseFontSizeSp
            ) { onConfigChange(config.copy(baseFontSizeSp = it)) }

            SectionTitle(R.string.paypal_saved_payment_method_style_container)
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_height,
                unit = "dp",
                defaultValue = 48f,
                range = 0f..200f,
                value = config.heightDp
            ) { onConfigChange(config.copy(heightDp = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_horizontal_padding,
                unit = "dp",
                defaultValue = 0f,
                value = config.horizontalPaddingDp
            ) { onConfigChange(config.copy(horizontalPaddingDp = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_vertical_padding,
                unit = "dp",
                defaultValue = 10f,
                value = config.verticalPaddingDp
            ) { onConfigChange(config.copy(verticalPaddingDp = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_corner_radius,
                unit = "dp",
                defaultValue = 0f,
                range = 0f..40f,
                value = config.cornerRadiusDp
            ) { onConfigChange(config.copy(cornerRadiusDp = it)) }
            OptionalColorRow(
                title = R.string.paypal_saved_payment_method_style_border_color,
                defaultColor = GRAY,
                value = config.borderColor
            ) { onConfigChange(config.copy(borderColor = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_border_width,
                unit = "dp",
                defaultValue = 1f,
                range = 0f..10f,
                value = config.borderWidthDp
            ) { onConfigChange(config.copy(borderWidthDp = it)) }

            SectionTitle(R.string.paypal_saved_payment_method_style_logo_and_label)
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_logo_width,
                unit = "dp",
                defaultValue = 48f,
                range = 0f..120f,
                value = config.logoWidthDp
            ) { onConfigChange(config.copy(logoWidthDp = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_label_font_size,
                unit = "sp",
                defaultValue = 20f,
                value = config.labelFontSizeSp
            ) { onConfigChange(config.copy(labelFontSizeSp = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_label_start_margin,
                unit = "dp",
                defaultValue = 6f,
                value = config.labelMarginStartDp
            ) { onConfigChange(config.copy(labelMarginStartDp = it)) }

            SectionTitle(R.string.paypal_saved_payment_method_style_funding_instrument)
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_text_font_size,
                unit = "sp",
                defaultValue = 14f,
                value = config.fundingInstrumentTextFontSizeSp
            ) { onConfigChange(config.copy(fundingInstrumentTextFontSizeSp = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_edit_icon_size,
                unit = "dp",
                defaultValue = 16f,
                value = config.editIconSizeDp
            ) { onConfigChange(config.copy(editIconSizeDp = it)) }
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_start_margin,
                unit = "dp",
                defaultValue = 12f,
                value = config.fundingInstrumentMarginStartDp
            ) { onConfigChange(config.copy(fundingInstrumentMarginStartDp = it)) }

            SectionTitle(R.string.paypal_saved_payment_method_style_credit_messaging)
            OptionalMetricRow(
                title = R.string.paypal_saved_payment_method_style_font_size,
                unit = "sp",
                defaultValue = 16f,
                value = config.creditMessagingFontSizeSp
            ) { onConfigChange(config.copy(creditMessagingFontSizeSp = it)) }
            OptionalColorRow(
                title = R.string.paypal_saved_payment_method_style_link_color,
                defaultColor = BLUE,
                value = config.creditMessagingLinkColor
            ) { onConfigChange(config.copy(creditMessagingLinkColor = it)) }
        }
    }
}

@Composable
internal fun SwitchRow(@StringRes label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(label))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Spacer(Modifier.size(8.dp))
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

/** A size that may be left unset. Off hands `null` to the SDK; on shows a slider. */
@Composable
private fun OptionalMetricRow(
    @StringRes title: Int,
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
    @StringRes title: Int,
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
