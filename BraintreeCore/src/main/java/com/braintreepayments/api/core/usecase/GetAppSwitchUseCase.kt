package com.braintreepayments.api.core.usecase

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.AppSwitchRepository

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetAppSwitchUseCase(private val appSwitchRepository: AppSwitchRepository) {
    operator fun invoke(): Boolean {
        return appSwitchRepository.isAppSwitchFlow
    }
}
