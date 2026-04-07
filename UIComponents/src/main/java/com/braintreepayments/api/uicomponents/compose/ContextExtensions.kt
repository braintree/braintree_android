package com.braintreepayments.api.uicomponents.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
