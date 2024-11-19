package com.braintreepayments.api.core

/**
 * DTO for analytics events. See also: [AnalyticsEventParams]
 * This class is internal to core module and is used in [AnalyticsClient] to construct the analytics
 * payload to be sent to the backend.
 */
internal data class AnalyticsEvent(
    val name: String,
    val timestamp: Long,
    val payPalContextId: String? = null,
    val linkType: String? = null,
    val isVaultRequest: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val endpoint: String? = null,
    val experiment: String? = null,
    val paymentMethodsDisplayed: List<String> = emptyList(),
    val appSwitchUrl: String? = null
)
