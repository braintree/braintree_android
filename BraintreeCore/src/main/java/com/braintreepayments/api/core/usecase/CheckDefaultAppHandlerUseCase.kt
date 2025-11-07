package com.braintreepayments.api.core.usecase

import com.braintreepayments.api.core.MerchantRepository

/**
 * Use to check if the merchant app can handle return link by default.
 */
class CheckDefaultAppHandlerUseCase(
    private val merchantRepository: MerchantRepository,
    private val getDefaultAppUseCase: GetDefaultAppUseCase = GetDefaultAppUseCase()
) {

    val context = merchantRepository.applicationContext

    operator fun invoke(): Boolean = context.packageName == getDefaultAppUseCase(
        merchantRepository.applicationContext.packageManager,
        merchantRepository.appLinkReturnUri
    )
}
