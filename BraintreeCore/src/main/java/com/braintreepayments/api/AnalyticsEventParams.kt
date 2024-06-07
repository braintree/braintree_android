package com.braintreepayments.api

public data class AnalyticsEventParams private constructor(
    val payPalContextId: String?,
    val linkType: String?,
    val isVaultRequest: Boolean,
) {
    class Builder {
        private var payPalContextId: String? = null
        private var linkType: String? = null
        private var isVaultRequest: Boolean = false

        fun setPayPalContextId(payPalContextId: String?) = apply { this.payPalContextId = payPalContextId }
        fun setLinkType(linkType: String?) = apply { this.linkType = linkType }
        fun setIsVaultRequest(isVaultRequest: Boolean) = apply { this.isVaultRequest = isVaultRequest }
        fun build() = AnalyticsEventParams(payPalContextId, linkType, isVaultRequest)
    }
}