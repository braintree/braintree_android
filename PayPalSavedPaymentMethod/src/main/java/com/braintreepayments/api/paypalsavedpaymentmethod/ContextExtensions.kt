package com.braintreepayments.api.paypalsavedpaymentmethod

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri

/** The activity this context belongs to, or `null` if it isn't an activity or wraps none. */
internal fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context != null) {
        if (context is Activity) return context
        // A wrapper that isn't attached yet has no base context.
        context = (context as? ContextWrapper)?.baseContext
    }
    return null
}

/** Opens [url] in the browser. Does nothing if no app can open it. */
internal fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    // Starting an activity from outside an activity needs a new task.
    if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (ignored: ActivityNotFoundException) {
        // No browser installed; nothing to open.
    }
}
