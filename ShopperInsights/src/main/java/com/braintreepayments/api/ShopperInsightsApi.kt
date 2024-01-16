package com.braintreepayments.api

import ShopperInsightApiResult

/**
 * [ShopperInsightsApi] is a wrapper class for all api related classes that
 * [ShopperInsightsClient] depends on.
 */
internal class ShopperInsightsApi(
    private val paymentsApi: FindEligiblePaymentsApi
) {

    fun findEligiblePayments(request: ShopperInsightsApiRequest): ShopperInsightApiResult {
        return paymentsApi.execute(request)
    }
}
