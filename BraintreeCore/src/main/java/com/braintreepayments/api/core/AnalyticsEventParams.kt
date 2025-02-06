package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * DTO for analytics events. See also: [AnalyticsEvent]
 * It is a catch-all data class for any parameters any of the modules wants to send. As such, not
 * all parameters are required at each call site.
 *
 * @property payPalContextId Used for linking events from the client to server side request.
 * @property linkType Indicates whether a deeplink or an app link was used to launch the app. Also see [LinkType].
 * @property isVaultRequest Indicates whether the request was a BillingAgreement(BA)/Vault request.
 * @property startTime [HttpResponseTiming] start time.
 * @property endTime [HttpResponseTiming] end time.
 * @property endpoint The endpoint being called.
 * @property experiment Currently a ShopperInsights module specific event that indicates
 * the experiment, as a JSON string, that the merchant sent to the us.
 * @property shopperSessionId The Shopper Insights customer session ID created by a merchant's
 * server SDK or graphQL integration.
 * @property buttonType buttonType Represents the tapped button type.
 * @property buttonOrder The order or ranking in which payment buttons appear.
 * @property pageType The page or view that a button is displayed on.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams @JvmOverloads constructor(
    val payPalContextId: String? = null,
    val linkType: String? = null,
    val isVaultRequest: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val endpoint: String? = null,
    val experiment: String? = null,
    val appSwitchUrl: String? = null,
    val shopperSessionId: String? = null,
    val buttonType: String? = null,
    val buttonOrder: String? = null,
    val pageType: String? = null
)
