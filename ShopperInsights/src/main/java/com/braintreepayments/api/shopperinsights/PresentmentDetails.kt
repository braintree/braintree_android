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


/*
Add new PresentmentDetails class with the following provided in the initializer:
experimentType - this will be represented by a new ExperimentType enum
This enum will be formatted and sent to FPTI as the experiment tag under the hood as:
 */

/*
ShopperInsightsClient.sendPayPalPresentedEvent
ShopperInsightsClient.sendVenmoPresentedEvent

Update the signature for sendPayPalPresentedEvent and sendVenmoPresentedEvent to sendPresentedEvent(for buttonType: BTButtonType, presentmentDetails: BTPresentmentDetails) - if this naming doesn’t translate well to Android use whatever pattern is best for this Android wise!
Verify that the above is being passed to FPTI as expected (verify with Arpan)
As needed update demo app
Open a PR with the above changes
 */