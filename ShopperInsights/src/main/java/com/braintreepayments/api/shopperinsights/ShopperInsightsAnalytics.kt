package com.braintreepayments.api.shopperinsights

internal object ShopperInsightsAnalytics {
    const val PAYPAL_SELECTED = "shopper-insights:paypal-selected"
    const val VENMO_SELECTED = "shopper-insights:venmo-selected"
    const val BUTTON_PRESENTED = "shopper-insights:button-presented"

    const val GET_RECOMMENDED_PAYMENTS_FAILED = "shopper-insights:get-recommended-payments:failed"
    const val GET_RECOMMENDED_PAYMENTS_STARTED = "shopper-insights:get-recommended-payments:started"
    const val GET_RECOMMENDED_PAYMENTS_SUCCEEDED = "shopper-insights:get-recommended-payments:succeeded"
}
