package com.braintreepayments.api.core.usecase

import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.core.net.toUri
import com.braintreepayments.api.core.CheckoutUri
import com.braintreepayments.api.core.MerchantRepository

/**
 * Use case that returns a return link type that will be used for navigating from App Switch or browser back into the
 * merchant app.
 *
 * Returns [ReturnLinkTypeResult.APP_LINK] if the default browser on the user device supports app links and user has
 * "Open supported links" checked for the merchant app. Otherwise returns [ReturnLinkTypeResult.DEEP_LINK].
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

    operator fun invoke(@CheckoutUri uri: Uri = "https://www.paypal.com/checkout".toUri()): ReturnLinkTypeResult {
        return if (checkReturnUriDefaultAppHandler() &&
            (canFirstPartyAppHandleAppLink(uri) || getAppLinksCompatibleBrowserUseCase(uri))
        ) {
            ReturnLinkTypeResult.APP_LINK
        } else {
            ReturnLinkTypeResult.DEEP_LINK
        }
    }

    /**
     * Checks if merchant app is able to handle return uri by default.
     * Returns false when user unchecks the "Open supported links" checkbox in the Android OS settings for the
     * merchant's app.
     */
    private fun checkReturnUriDefaultAppHandler(): Boolean =
        merchantRepository.applicationContext.packageName == getDefaultAppUseCase(merchantRepository.appLinkReturnUri)

    /**
     * Checks if the PayPal app can handle the checkout uri. Returns true if the PayPal app can properly handle the
     * checkout uri, false otherwise.
     */
    private fun canFirstPartyAppHandleAppLink(@CheckoutUri uri: Uri): Boolean {
        return FIRST_PARTY_PACKAGE_NAMES.any { it ->
            getDefaultAppUseCase(uri)?.contains(it) == true
        }
    }

    private companion object {
        private const val PAYPAL_APP_PACKAGE_NAME = "com.paypal.android.p2pmobile"
        private const val VENMO_APP_PACKAGE_NAME = "com.venmo"
        private val FIRST_PARTY_PACKAGE_NAMES = setOf<String>(PAYPAL_APP_PACKAGE_NAME, VENMO_APP_PACKAGE_NAME)
    }
}
