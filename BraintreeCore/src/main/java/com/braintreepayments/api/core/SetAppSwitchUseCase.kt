package com.braintreepayments.api.core

class SetAppSwitchUseCase(private val appSwitchRepository: AppSwitchRepository) {

    operator fun invoke(appSwitchFlow: Boolean) {
        appSwitchRepository.isAppSwitchFlow = appSwitchFlow
    }
}
