package com.braintreepayments.api.shopperinsights

/**
 * Initializes a new PresentmentDetails instance
 *
 * @property type An ExperimentType that is either a control or test type
 * @property buttonOrder Represents this buttons order in context of other buttons.
 * @property pageType Represents the page or view the button is rendered on.
 */
data class PresentmentDetails(
    val type: ExperimentType,
    val buttonOrder: ButtonOrder,
    val pageType: PageType
)
