package com.braintreepayments.api.core

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

/**
 * Use case that returns the default browser that should be used for navigating from App Switch or browser back into the
 * merchant app.
 */
class GetDefaultBrowserUseCase(private val merchantRepository: MerchantRepository) {

    operator fun invoke(): String? {
        val context = merchantRepository.applicationContext
        val browserIntent = Intent(Intent.ACTION_VIEW, merchantRepository.appLinkReturnUri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolveInfo: ResolveInfo? = context.packageManager.resolveActivity(
            browserIntent, PackageManager
                .MATCH_DEFAULT_ONLY
        )

        if (resolveInfo != null && resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName
        }
        return null // No default browser found or resolved
    }
}
