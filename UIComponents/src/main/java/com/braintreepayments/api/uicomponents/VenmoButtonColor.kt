package com.braintreepayments.api.uicomponents

import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt

/**
 * Enum representing the different colors of Venmo buttons
 */
enum class VenmoButtonColor (
        val key: Int,
        @ColorInt val fill: Int,
        @ColorInt val border: Int,
        val logoId: Int
    ) {
        BLUE(0, "#008CFF".toColorInt(), "#008CFF".toColorInt(), R.drawable.venmo_logo_white),
        BLACK(1, "#000000".toColorInt(), "#000000".toColorInt(), R.drawable.venmo_logo_white),
        WHITE(2, "#FFFFFF".toColorInt(), "#555555".toColorInt(), R.drawable.venmo_logo_blue);

        companion object {
        internal fun fromId(id: Int): VenmoButtonColor {
            return VenmoButtonColor.entries.firstOrNull { it.key == id } ?: BLUE
        }
    }
}