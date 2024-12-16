package com.braintreepayments.api.shopperinsights


/**
 * Initializes a new PresentmentDetails instance
 *
 * @param type An ExperimentType that is either a control or test type
 * @param buttonOrder optional Represents this buttons order in context of other buttons.
 * @param pageType optional Represents the page or view the button is rendered on.
 */
data class PresentmentDetails (
    val type: ExperimentType,
    val buttonOrder: ButtonOrder,
    val pageType: PageType
)