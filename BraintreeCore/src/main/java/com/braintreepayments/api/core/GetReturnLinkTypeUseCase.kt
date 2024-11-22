package com.braintreepayments.api.core

import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase.ReturnLinkType

/**
 * Use case that returns which link type should be used for navigating from App Switch / CCT back into the merchant app.
 *
 * If a user unchecks the "Open supported links" checkbox in the Android OS settings for the merchant's app. If this
 * setting is unchecked, this use case will return [ReturnLinkType.DEEP_LINK], otherwise [ReturnLinkType.APP_LINK]
 * will be returned.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnLinkTypeUseCase(private val merchantRepository: MerchantRepository) {

    enum class ReturnLinkType { APP_LINK, DEEP_LINK }

    operator fun invoke(): ReturnLinkType {
        val context = merchantRepository.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, merchantRepository.appLinkReturnUri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolvedActivity = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return if (resolvedActivity?.activityInfo?.packageName == context.packageName) {
            ReturnLinkType.APP_LINK
        } else {
            ReturnLinkType.DEEP_LINK
        }
    }
}
