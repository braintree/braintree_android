package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import kotlin.math.roundToInt

/** Converts dp to whole pixels; a positive value is at least 1 pixel, so a thin border never disappears. */
internal fun Float.dpToPx(resources: Resources): Int {
    val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics).roundToInt()
    return if (this > 0f) px.coerceAtLeast(1) else px
}

/** The weight of a text element. A custom font only gets [MEDIUM] on Android 9 and later. */
internal enum class TextWeight { REGULAR, MEDIUM }

/** Applies the merchant's font, or the system font, at [sizeSp] and [weight] in [textColor]. */
internal fun TextView.applyTextStyle(
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    sizeSp: Float,
    weight: TextWeight = TextWeight.REGULAR,
    @ColorInt textColor: Int = resolvedStyle.textColor
) {
    setTextColor(textColor)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    val font = resolvedStyle.fontResId?.let { fontOrNull(it) }
    typeface = when {
        weight == TextWeight.REGULAR -> font ?: Typeface.DEFAULT
        font == null -> Typeface.create(SYSTEM_MEDIUM_FONT_FAMILY, Typeface.NORMAL)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> Typeface.create(font, MEDIUM_WEIGHT, false)
        else -> font
    }
}

private fun TextView.fontOrNull(@FontRes fontResId: Int): Typeface? =
    try {
        ResourcesCompat.getFont(context, fontResId)
    } catch (ignored: Resources.NotFoundException) {
        null
    }

private const val SYSTEM_MEDIUM_FONT_FAMILY = "sans-serif-medium"
private const val MEDIUM_WEIGHT = 500
