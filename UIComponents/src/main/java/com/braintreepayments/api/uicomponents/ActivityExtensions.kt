package com.braintreepayments.api.uicomponents

import android.content.ContextWrapper
import android.view.View
import androidx.activity.ComponentActivity

internal fun View.getActivity(): ComponentActivity? {
    var context = this.context
    while (context is ContextWrapper) {
        if (context is ComponentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}
