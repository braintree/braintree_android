package com.braintreepayments.api.uicomponents

import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt

enum class PayPalButtonStyle(
        val key: String,
        @ColorInt val fill: Int,
        @ColorInt val border: Int,
        val logoId: Int
    ) {
        WHITE("white", "#FFFFFF".toColorInt(), "#555555".toColorInt(), R.drawable.paypal_logo_black),
        BLACK("black", "#000000".toColorInt(), "#000000".toColorInt(), R.drawable.paypal_logo_white),
        BLUE("blue", "#60CDFF".toColorInt(), "#60CDFF".toColorInt(), R.drawable.paypal_logo_black);

        companion object {
            internal fun fromString(stringValue: String?): PayPalButtonStyle {
                val value = stringValue?.lowercase()
                return PayPalButtonStyle.entries.firstOrNull { it.key == value } ?: BLUE
            }
        }
    }
