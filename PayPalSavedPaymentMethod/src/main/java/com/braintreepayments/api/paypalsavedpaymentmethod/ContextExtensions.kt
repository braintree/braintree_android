package com.braintreepayments.api.paypalsavedpaymentmethod

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import com.braintreepayments.api.paypalsavedpaymentmethod.state.PayPalCreditMessagingContent

/**
 * The activity this context belongs to, or `null` if it isn't an activity or wraps none. A wrapper that isn't attached
 * yet has no base context.
 */
internal fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context != null) {
        if (context is Activity) return context
        context = (context as? ContextWrapper)?.baseContext
    }
    return null
}

/**
 * Opens the "Learn more" page. A page PayPal allows embedding opens in a Custom Tab, as iOS opens it in a Safari view
 * controller; any other page, or one with no Custom Tabs browser, opens in the browser. The Custom Tabs session extra
 * is set directly, as CustomTabsIntent does, so the module doesn't depend on androidx.browser. Outside an activity it
 * starts a new task. Does nothing if no app can open it.
 */
internal fun Context.openLearnMore(learnMore: PayPalCreditMessagingContent.LearnMore) {
    val intent = Intent(Intent.ACTION_VIEW, learnMore.url.toUri())
    if (learnMore.isEmbeddable) intent.putExtras(Bundle().apply { putBinder(CUSTOM_TABS_SESSION_EXTRA, null) })
    if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (ignored: ActivityNotFoundException) {
        // No app can open it.
    }
}

/** CustomTabsIntent.EXTRA_SESSION. A null session opens a Custom Tab without a service connection. */
private const val CUSTOM_TABS_SESSION_EXTRA = "android.support.customtabs.extra.SESSION"
