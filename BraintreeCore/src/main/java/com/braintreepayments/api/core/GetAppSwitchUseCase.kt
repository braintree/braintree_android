package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetAppSwitchUseCase(private val appSwitchRepository: AppSwitchRepository) {
    operator fun invoke(): Boolean {
        return appSwitchRepository.isAppSwitchFlow
    }
}
