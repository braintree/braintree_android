package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import kotlin.math.roundToInt

/** Converts a dp value from the style resolver to whole pixels, rounded so a thin border never disappears. */
internal fun Float.dpToPx(resources: Resources): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics).roundToInt()

/**
 * Applies the merchant's font, or the system font, at [sizeSp] in the style's text color. [medium] uses the medium
 * weight, which a custom font only gets on Android 9 and later.
 */
internal fun TextView.applyTextStyle(
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    sizeSp: Float,
    medium: Boolean = false
) {
    setTextColor(resolvedStyle.textColor)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    val font = resolvedStyle.fontResId?.let { ResourcesCompat.getFont(context, it) }
    typeface = when {
        !medium -> font ?: Typeface.DEFAULT
        font == null -> Typeface.create(SYSTEM_MEDIUM_FONT_FAMILY, Typeface.NORMAL)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> Typeface.create(font, MEDIUM_WEIGHT, false)
        else -> font
    }
}

private const val SYSTEM_MEDIUM_FONT_FAMILY = "sans-serif-medium"
private const val MEDIUM_WEIGHT = 500
