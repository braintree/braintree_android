package com.braintreepayments.api.core.usecase

import android.net.Uri
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.CheckoutUri

/**
 * Checks whether the default browser of the device is compatible with app links feature based on a static list of
 * pre-tested browsers for app links compatibility.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetAppLinksCompatibleBrowserUseCase(
    private val getDefaultAppUseCase: GetDefaultAppUseCase,
) {

    operator fun invoke(@CheckoutUri browserUri: Uri?): Boolean =
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
