package com.braintreepayments.api.core.usecase

import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.MerchantRepository

/**
 * Use case that returns a return link that should be used for navigating from App Switch / CCT back into the merchant
 * app. It handles both App Links and Deep Links.
 *
 * If a user unchecks the "Open supported links" checkbox in the Android OS settings for the merchant's app. If this
 * setting is unchecked, this use case will return [ReturnLinkResult.DeepLink], otherwise [ReturnLinkResult.AppLink]
 * will be returned.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnLinkUseCase(
    private val merchantRepository: MerchantRepository,
    getDefaultBrowserUseCase: GetDefaultBrowserUseCase = GetDefaultBrowserUseCase(
        merchantRepository.applicationContext.packageManager
    ),
    getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase =
        GetAppLinksCompatibleBrowserUseCase(getDefaultBrowserUseCase),
    getDefaultAppUseCase: GetDefaultAppUseCase = GetDefaultAppUseCase(merchantRepository.applicationContext.packageManager),
    private val getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase = GetReturnLinkTypeUseCase(
        merchantRepository,
        getDefaultAppUseCase,
        getAppLinksCompatibleBrowserUseCase
    ),
) {

    sealed class ReturnLinkResult {
        data class AppLink(val appLinkReturnUri: Uri) : ReturnLinkResult()

        data class DeepLink(val deepLinkFallbackUrlScheme: String) : ReturnLinkResult()

        data class Failure(val exception: Exception) : ReturnLinkResult()
    }

    /**
     * [uri] - [internal - remove before publish] The url to be sent here is the checkout url that the browser
     * opens, not the merchant passed return url
     */
    operator fun invoke(uri: Uri? = "https://example.com".toUri()): ReturnLinkResult {
        return when (getReturnLinkTypeUseCase(uri)) {
            GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK -> {
                merchantRepository.appLinkReturnUri?.let { ReturnLinkResult.AppLink(it) }
                    ?: ReturnLinkResult.Failure(BraintreeException("App Link Return Uri is null"))
            }

            GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK -> {
                merchantRepository.deepLinkFallbackUrlScheme?.let { ReturnLinkResult.DeepLink(it) }
                    ?: ReturnLinkResult.Failure(BraintreeException("Deep Link fallback return url is null"))
            }
        }
    }
}
