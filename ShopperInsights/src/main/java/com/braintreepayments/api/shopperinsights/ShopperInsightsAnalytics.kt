package com.braintreepayments.api.shopperinsights

internal object ShopperInsightsAnalytics {
    const val BUTTON_SELECTED = "shopper-insights:button-selected"
    const val BUTTON_PRESENTED = "shopper-insights:button-presented"

    const val GET_RECOMMENDED_PAYMENTS_FAILED = "shopper-insights:get-recommended-payments:failed"
    const val GET_RECOMMENDED_PAYMENTS_STARTED = "shopper-insights:get-recommended-payments:started"
    const val GET_RECOMMENDED_PAYMENTS_SUCCEEDED = "shopper-insights:get-recommended-payments:succeeded"

    const val CREATE_CUSTOMER_SESSION_STARTED = "shopper-insights:create-customer-session:started"
    const val CREATE_CUSTOMER_SESSION_SUCCEEDED = "shopper-insights:create-customer-session:succeeded"
    const val CREATE_CUSTOMER_SESSION_FAILED = "shopper-insights:create-customer-session:failed"

    const val UPDATE_CUSTOMER_SESSION_STARTED = "shopper-insights:update-customer-session:started"
    const val UPDATE_CUSTOMER_SESSION_SUCCEEDED = "shopper-insights:update-customer-session:succeeded"
    const val UPDATE_CUSTOMER_SESSION_FAILED = "shopper-insights:update-customer-session:failed"

    const val GET_CUSTOMER_RECOMMENDATIONS_STARTED = "shopper-insights:get-customer-recommendations:started"
    const val GET_CUSTOMER_RECOMMENDATIONS_SUCCEEDED = "shopper-insights:get-customer-recommendations:succeeded"
    const val GET_CUSTOMER_RECOMMENDATIONS_FAILED = "shopper-insights:get-customer-recommendations:failed"

    const val MANAGE_CUSTOMER_SESSION_WITH_RECOMMENDATION_STARTED =
        "shopper-insights:manage-customer-session-with-recommendations:started"
    const val MANAGE_CUSTOMER_SESSION_WITH_RECOMMENDATION_SUCCEEDED =
        "shopper-insights:manage-customer-session-with-recommendations:succeeded"
    const val MANAGE_CUSTOMER_SESSION_WITH_RECOMMENDATION_FAILED =
        "shopper-insights:manage-customer-session-with-recommendations:failed"
}
