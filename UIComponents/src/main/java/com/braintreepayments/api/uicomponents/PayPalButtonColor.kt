package com.braintreepayments.api.uicomponents

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt

/**
 * A representation of the different colors of PayPal buttons
 */
@Suppress("MagicNumber", "CyclomaticComplexMethod")
sealed class PayPalButtonColor(
    open val key: Int,
    @ColorInt open val fill: Int,
    @ColorInt open val border: Int,
    @ColorInt open val focusIndicator: Int = Color.argb(0, 0, 0, 0),
    val logoId: Int
) {
    sealed class Blue(
        override val key: Int,
        @ColorInt override val fill: Int,
        @ColorInt override val border: Int,
        @ColorInt override val focusIndicator: Int = Color.argb(0, 0, 0, 0)
    ) : PayPalButtonColor(key, fill, border, focusIndicator, R.drawable.paypal_logo_black) {
        object Default : Blue(0, "#60CDFF".toColorInt(), "#60CDFF".toColorInt())
        object Hover : Blue(3, "#54B4E0".toColorInt(), "#54B4E0".toColorInt())
        object Focus : Blue(6, "#60CDFF".toColorInt(), "#60CDFF".toColorInt(), "#0066F5".toColorInt())
        object FocusHover : Blue(9, "#54B4E0".toColorInt(), "#54B4E0".toColorInt(), "#0066F5".toColorInt())
        object Pressed : Blue(12, "#3DB5FF".toColorInt(), "#3DB5FF".toColorInt())
    }

    sealed class Black(
        override val key: Int,
        @ColorInt override val fill: Int,
        @ColorInt override val border: Int,
        @ColorInt override val focusIndicator: Int = Color.argb(0, 0, 0, 0)
    ) : PayPalButtonColor(key, fill, border, focusIndicator, R.drawable.paypal_logo_white) {
        object Default : Black(1, "#000000".toColorInt(), "#000000".toColorInt())
        object Hover : Black(4, "#333333".toColorInt(), "#333333".toColorInt())
        object Focus : Black(7, "#000000".toColorInt(), "#000000".toColorInt(), "#0066F5".toColorInt())
        object FocusHover : Black(10, "#333333".toColorInt(), "#333333".toColorInt(), "#0066F5".toColorInt())
        object Pressed : Black(13, "#696969".toColorInt(), "#696969".toColorInt())
    }

    sealed class White(
        override val key: Int,
        @ColorInt override val fill: Int,
        @ColorInt override val border: Int,
        @ColorInt override val focusIndicator: Int = Color.argb(0, 0, 0, 0)
    ) : PayPalButtonColor(key, fill, border, focusIndicator, R.drawable.paypal_logo_black) {
        object Default : White(2, "#FFFFFF".toColorInt(), "#555555".toColorInt())
        object Hover : White(5, "#F2F2F2".toColorInt(), "#555555".toColorInt())
        object Focus : White(8, "#FFFFFF".toColorInt(), "#555555".toColorInt(), "#0066F5".toColorInt())
        object FocusHover : White(11, "#F2F2F2".toColorInt(), "#555555".toColorInt(), "#0066F5".toColorInt())
        object Pressed : White(14, "#E9E9E9".toColorInt(), "#555555".toColorInt())
    }

    companion object {
        internal fun fromId(id: Int): PayPalButtonColor = when (id) {
            0 -> Blue.Default
            1 -> Black.Default
            2 -> White.Default
            3 -> Blue.Hover
            4 -> Black.Hover
            5 -> White.Hover
            6 -> Blue.Focus
            7 -> Black.Focus
            8 -> White.Focus
            9 -> Blue.FocusHover
            10 -> Black.FocusHover
            11 -> White.FocusHover
            12 -> Blue.Pressed
            13 -> Black.Pressed
            14 -> White.Pressed
            else -> Blue.Default
        }

        internal fun baseStyle(color: PayPalButtonColor): PayPalButtonColor = fromId(color.key % 3)
    }
}
