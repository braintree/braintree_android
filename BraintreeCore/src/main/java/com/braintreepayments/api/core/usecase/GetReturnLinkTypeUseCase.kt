package com.braintreepayments.api.core.usecase

import android.net.Uri
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
class GetReturnLinkTypeUseCase(
    private val checkDefaultAppHandlerUseCase: CheckDefaultAppHandlerUseCase,
    private val getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase
) {

    enum class ReturnLinkTypeResult {
        APP_LINK, DEEP_LINK
    }

    operator fun invoke(uri: Uri?): ReturnLinkTypeResult {
        return if (checkDefaultAppHandlerUseCase() && getAppLinksCompatibleBrowserUseCase(uri)) {
            ReturnLinkTypeResult.APP_LINK
        } else {
            ReturnLinkTypeResult.DEEP_LINK
        }
    }
}
