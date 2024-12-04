package com.braintreepayments.api.core

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.GetReturnLinkUseCase.ReturnLinkResult

/**
 * Use case that returns a return link that should be used for navigating from App Switch / CCT back into the merchant
 * app. It handles both App Links and Deep Links.
 *
 * If a user unchecks the "Open supported links" checkbox in the Android OS settings for the merchant's app. If this
 * setting is unchecked, this use case will return [ReturnLinkResult.DeepLink], otherwise [ReturnLinkResult.AppLink]
 * will be returned.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnLinkUseCase(private val merchantRepository: MerchantRepository) {

    sealed class ReturnLinkResult {
        data class AppLink(val appLinkReturnUri: Uri) : ReturnLinkResult()

        data class DeepLink(val deepLinkFallbackUrlScheme: String) : ReturnLinkResult()

        data class Failure(val exception: Exception) : ReturnLinkResult()
    }

    operator fun invoke(): ReturnLinkResult {
        val context = merchantRepository.applicationContext
        val intent = Intent(Intent.ACTION_VIEW, merchantRepository.appLinkReturnUri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolvedActivity = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return if (resolvedActivity?.activityInfo?.packageName == context.packageName) {
            merchantRepository.appLinkReturnUri?.let {
                ReturnLinkResult.AppLink(it)
            } ?: run {
                ReturnLinkResult.Failure(BraintreeException("App Link Return Uri is null"))
            }
        } else {
            merchantRepository.deepLinkFallbackUrlScheme?.let {
                ReturnLinkResult.DeepLink(it)
            } ?: run {
                ReturnLinkResult.Failure(BraintreeException("Deep Link fallback return url is null"))
            }
        }
    }
}
