package com.braintreepayments.api.core.usecase

import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.braintreepayments.api.core.MerchantRepository

/**
 * Use case that returns a return link type that will be used for navigating from App Switch or browser back into the
 * merchant app.
 *
 * If a user unchecks the "Open supported links" checkbox in the Android OS settings for the merchant's app. If this
 * setting is unchecked, this use case will return [ReturnLinkTypeResult.DEEP_LINK], otherwise
 * [ReturnLinkTypeResult.APP_LINK] will be returned.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetReturnLinkTypeUseCase(
    private val merchantRepository: MerchantRepository,
    private val getDefaultAppUseCase: GetDefaultAppUseCase,
    private val getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase
) {

    enum class ReturnLinkTypeResult {
        APP_LINK, DEEP_LINK
    }

    /**
     * [uri] - [internal - remove before publish] The url to be sent here is the checkout url that the browser
     * opens, not the merchant passed return url
     */
    operator fun invoke(uri: Uri? = "https://example.com".toUri()): ReturnLinkTypeResult {
        return if (checkReturnUriDefaultAppHandler() && getAppLinksCompatibleBrowserUseCase(uri)) {
            ReturnLinkTypeResult.APP_LINK
        } else {
            ReturnLinkTypeResult.DEEP_LINK
        }
    }

    private fun checkReturnUriDefaultAppHandler(): Boolean =
        merchantRepository.applicationContext.packageName == getDefaultAppUseCase(merchantRepository.appLinkReturnUri)
}
