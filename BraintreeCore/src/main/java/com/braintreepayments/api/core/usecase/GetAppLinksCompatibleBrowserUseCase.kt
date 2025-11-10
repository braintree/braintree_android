package com.braintreepayments.api.core.usecase

import android.net.Uri
import androidx.annotation.RestrictTo

/**
 * Checks whether the default browser of the device is compatible with app links feature based on a static list of
 * pre-tested browsers for app links compatibility.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetAppLinksCompatibleBrowserUseCase(
    private val getDefaultAppUseCase: GetDefaultAppUseCase,
) {

    /**
     * [browserUri] - [internal - remove before publish] The url to be sent here is the checkout url that the browser
     * opens, not the merchant passed return url.
     */
    operator fun invoke(browserUri: Uri?): Boolean =
        appLinkCompatibleBrowsers.any { getDefaultAppUseCase(browserUri)?.contains(it) == true }

    companion object {
        private val appLinkCompatibleBrowsers = listOf(
            "com.android.chrome",
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx"
        )
    }
}
