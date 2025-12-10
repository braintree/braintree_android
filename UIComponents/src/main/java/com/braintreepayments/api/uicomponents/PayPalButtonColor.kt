package com.braintreepayments.api.uicomponents

import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt

/**
 * Enum representing the different colors of PayPal buttons
 */
@Suppress("MaxLineLength")
enum class PayPalButtonColor(
        val key: Int,
        @ColorInt val fill: Int,
        @ColorInt val border: Int,
        val logoId: Int,
        val spinnerId: Int
    ) {
        BLUE(0, "#60CDFF".toColorInt(), "#60CDFF".toColorInt(), R.drawable.paypal_logo_black, R.drawable.avd_spinner_black),
        BLACK(1, "#000000".toColorInt(), "#000000".toColorInt(), R.drawable.paypal_logo_white, R.drawable.avd_spinner_white),
        WHITE(2, "#FFFFFF".toColorInt(), "#555555".toColorInt(), R.drawable.paypal_logo_black, R.drawable.avd_spinner_black);

        companion object {
            internal fun fromId(id: Int): PayPalButtonColor {
                return PayPalButtonColor.entries.firstOrNull { it.key == id } ?: BLUE
            }
        }
    }
