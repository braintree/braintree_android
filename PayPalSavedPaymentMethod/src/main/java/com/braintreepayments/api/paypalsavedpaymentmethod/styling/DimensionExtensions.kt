package com.braintreepayments.api.paypalsavedpaymentmethod.styling

import android.content.res.Resources
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DimenRes

/** Converts a pixel value to dp. */
internal fun Resources.pxToDp(px: Float): Float = px / displayMetrics.density

/** Converts a pixel value to sp, honoring the user's font scale (non-linear on Android 14+). */
internal fun Resources.pxToSp(px: Float): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_SP, px, displayMetrics)
    } else {
        @Suppress("DEPRECATION")
        px / displayMetrics.scaledDensity
    }

/** Reads a dimension resource in dp. */
internal fun Resources.dimenDp(@DimenRes dimenRes: Int): Float = pxToDp(getDimension(dimenRes))

/** Reads a dimension resource in sp. */
internal fun Resources.dimenSp(@DimenRes dimenRes: Int): Float = pxToSp(getDimension(dimenRes))
