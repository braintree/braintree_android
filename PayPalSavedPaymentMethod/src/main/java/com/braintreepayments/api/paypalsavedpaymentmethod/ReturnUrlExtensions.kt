package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Intent
import android.net.Uri

/**
 * Removes the PayPal return URL from this intent once its return is completed, so a later edit doesn't read it again.
 * Any other data, such as the merchant's own deep link, is left alone. The URL is matched as the browser switch does:
 * by scheme and host for the App Link, and by scheme for the deep link fallback.
 */
internal fun Intent.clearPayPalReturnUrl(appLinkReturnUrl: Uri, deepLinkFallbackUrlScheme: String?) {
    val url = data ?: return
    val isDeepLinkReturn = deepLinkFallbackUrlScheme != null &&
        url.scheme.equals(deepLinkFallbackUrlScheme, ignoreCase = true)
    val isAppLinkReturn = url.scheme.equals(appLinkReturnUrl.scheme, ignoreCase = true) &&
        url.host.equals(appLinkReturnUrl.host, ignoreCase = true)
    if (isDeepLinkReturn || isAppLinkReturn) data = null
}
