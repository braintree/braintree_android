package com.braintreepayments.api.core

class GetAppSwitchUseCase(private val appSwitchRepository: AppSwitchRepository) {
    operator fun invoke(): Boolean {
        return appSwitchRepository.isAppSwitchFlow
    }
}
