package com.braintreepayments.api.paypalsavedpaymentmethod

import androidx.core.net.toUri

// Server-supplied URLs are fired automatically or opened in a Custom Tab, so anything other than https is dropped.
internal fun String?.asHttpsUrlOrNull(): String? =
    this?.takeIf {
        val uri = it.toUri()
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrEmpty()
    }
