package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * DTO for analytics events. See also: [AnalyticsEvent]
 * It is a catch-all data class for any parameters any of the modules wants to send. As such, not
 * all parameters are required at each call site.
 *
 * @property contextId Used for linking events from the client to server side request.
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
 * @property errorDescription use this to provide more detail on a failure event.
 * @property uiType The UI integration type, for example `compose` or `xml`.
 * @property buttonSessionId An ID unique to each render of the saved payment method component, used to link its events
 * with PayPal's for the same render.
 * @property isEditFiFlow Whether a PayPal tokenize was started from the saved payment method edit flow.
 * @property savedMethodType The type of the saved payment method shown or selected, for example `CARD`.
 * @property savedMethodId PayPal's encrypted ID of the saved payment method shown or selected.
 * @property savedMethodBrand The brand of the saved card shown or selected, for example `VISA`.
 * @property presentmentMessageType The template of the Pay Later message shown, for example `PLST_SQ`.
 * @property renderTrigger What caused a component render: the initial fetch or the refetch after an edit.
 * @property shownContent What a component render showed, for example a funding instrument or only the brand.
 * @property renderedLogo Whether the PayPal logo was rendered.
 * @property renderedLabel Whether the PayPal label was rendered.
 * @property renderedEditTag Whether the edit icon was rendered.
 * @property renderedMessageSlot Whether the Pay Later message slot was rendered.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AnalyticsEventParams @JvmOverloads constructor(
    val contextId: String? = null,
    val isVaultRequest: Boolean = false,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val endpoint: String? = null,
    val experiment: String? = null,
    val appSwitchUrl: String? = null,
    val shopperSessionId: String? = null,
    val buttonType: String? = null,
    val buttonOrder: String? = null,
    val pageType: String? = null,
    val errorDescription: String? = null,
    val uiType: String? = null,
    val buttonSessionId: String? = null,
    val isEditFiFlow: Boolean? = null,
    val savedMethodType: String? = null,
    val savedMethodId: String? = null,
    val savedMethodBrand: String? = null,
    val presentmentMessageType: String? = null,
    val renderTrigger: String? = null,
    val shownContent: String? = null,
    val renderedLogo: Boolean? = null,
    val renderedLabel: Boolean? = null,
    val renderedEditTag: Boolean? = null,
    val renderedMessageSlot: Boolean? = null
)
