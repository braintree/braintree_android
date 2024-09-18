package com.braintreepayments.api.paypal.vaultedit

import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.ExperimentalBetaApi

@ExperimentalBetaApi
internal data class PayPalVaultEditAuthRequestParams(
    val browserSwitchOptions: BrowserSwitchOptions
)
