package com.braintreepayments.api.paypalsavedpaymentmethod.xml

import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.braintreepayments.api.paypalsavedpaymentmethod.styling.PayPalSavedPaymentMethodStyleResolver
import kotlin.math.roundToInt

/** Converts a dp value from the style resolver to whole pixels, rounded so a thin border never disappears. */
internal fun Float.dpToPx(resources: Resources): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics).roundToInt()

/**
 * The drawable a resolved theme attribute points to, or `null` when it has no resource ID. A theme can set an attribute
 * such as `selectableItemBackground` to a plain color, and loading resource ID 0 would crash.
 */
internal fun TypedValue.drawableOrNull(context: Context): Drawable? =
    if (resourceId != 0) ContextCompat.getDrawable(context, resourceId) else null

/**
 * Applies the merchant's font, or the system font, at [sizeSp] in [textColor], the style's text color by default.
 * [medium] uses the medium weight, which a custom font only gets on Android 9 and later. A font resource that can't be
 * loaded falls back to the system font.
 */
internal fun TextView.applyTextStyle(
    resolvedStyle: PayPalSavedPaymentMethodStyleResolver,
    sizeSp: Float,
    medium: Boolean = false,
    @ColorInt textColor: Int = resolvedStyle.textColor
) {
    setTextColor(textColor)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    val font = resolvedStyle.fontResId?.let { fontOrNull(it) }
    typeface = when {
        !medium -> font ?: Typeface.DEFAULT
        font == null -> Typeface.create(SYSTEM_MEDIUM_FONT_FAMILY, Typeface.NORMAL)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> Typeface.create(font, MEDIUM_WEIGHT, false)
        else -> font
    }
}

// The font attr accepts any resource reference, so a merchant can pass something that isn't a font. That must not
// crash the host app; the text uses the system font instead.
private fun TextView.fontOrNull(@FontRes fontResId: Int): Typeface? =
    try {
        ResourcesCompat.getFont(context, fontResId)
    } catch (ignored: Resources.NotFoundException) {
        null
    }

private const val SYSTEM_MEDIUM_FONT_FAMILY = "sans-serif-medium"
private const val MEDIUM_WEIGHT = 500
