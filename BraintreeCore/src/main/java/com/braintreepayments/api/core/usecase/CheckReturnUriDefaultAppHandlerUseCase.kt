package com.braintreepayments.api.core.usecase

import com.braintreepayments.api.core.MerchantRepository

/**
 * Use to check if the merchant app can handle return link by default.
 */
class CheckReturnUriDefaultAppHandlerUseCase(
    private val merchantRepository: MerchantRepository,
    private val getDefaultAppUseCase: GetDefaultAppUseCase = GetDefaultAppUseCase()
) {

    operator fun invoke(): Boolean = merchantRepository.applicationContext.packageName == getDefaultAppUseCase(
        merchantRepository.applicationContext.packageManager,
        merchantRepository.appLinkReturnUri
    )
}
