package com.braintreepayments.api.paypal.vaultedit

import androidx.annotation.RestrictTo
import com.braintreepayments.api.BrowserSwitchOptions

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PayPalVaultEditAuthRequestParams @JvmOverloads internal constructor(
    val clientMetadataId: String,
    var browserSwitchOptions: BrowserSwitchOptions?,
    val approvalUrl: String,
    val successUrl: String? = null,
)
