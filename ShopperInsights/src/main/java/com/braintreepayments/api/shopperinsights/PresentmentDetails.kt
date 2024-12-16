package com.braintreepayments.api.shopperinsights


/**
 * Initializes a new PresentmentDetails instance
 *
 * @param treatmentName The experiment or treatment name
 * @param type An ExperimentType that is either a control or test type
 */
data class PresentmentDetails (
    val treatmentName: String,
    val type: ExperimentType
)