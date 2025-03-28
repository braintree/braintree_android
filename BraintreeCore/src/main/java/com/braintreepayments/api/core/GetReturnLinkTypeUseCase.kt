package com.braintreepayments.api.core

import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo

/**
 * Use case that returns a return link type that will be used for navigating from App Switch or browser back into the
 * merchant app.
 *
 * If a user unchecks the "Open supported links" checkbox in the Android OS settings for the merchant's app. If this
 * setting is unchecked, this use case will return [ReturnLinkTypeResult.DEEP_LINK], otherwise
 * [ReturnLinkTypeResult.APP_LINK] will be returned.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnLinkTypeUseCase(private val merchantRepository: MerchantRepository) {

    enum class ReturnLinkTypeResult {
        APP_LINK, DEEP_LINK
    }

    operator fun invoke(): ReturnLinkTypeResult {
        val context = merchantRepository.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, merchantRepository.appLinkReturnUri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolvedActivity = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return if (resolvedActivity?.activityInfo?.packageName == context.packageName) {
            ReturnLinkTypeResult.APP_LINK
        } else {
            ReturnLinkTypeResult.DEEP_LINK
        }
    }
}
