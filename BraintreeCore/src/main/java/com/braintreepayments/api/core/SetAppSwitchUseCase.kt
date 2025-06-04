package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SetAppSwitchUseCase(
    private val appSwitchRepository: AppSwitchRepository,
    private val deviceInspector: DeviceInspector = DeviceInspectorProvider().deviceInspector
) {

    /**
     * Sets the status of the app switch flow. This should be called once the PayPal response is received. Since it is
     * the final check to see if the app switch flow should be shown.
     *
     * App Switch Logic:
     * 1. Merchant enabled app switch
     * 2. PayPal app installed
     * 3. PayPal response indicates an app switch flow
     *
     * @param appSwitchFlowFromPayPalResponse whether the PayPal response indicates an app switch flow.
     */
    operator fun invoke(
        merchantEnabledAppSwitch: Boolean,
        appSwitchFlowFromPayPalResponse: Boolean
    ) {
        appSwitchRepository.isAppSwitchFlow = merchantEnabledAppSwitch &&
            deviceInspector.isPayPalInstalled() &&
            appSwitchFlowFromPayPalResponse
    }
}
