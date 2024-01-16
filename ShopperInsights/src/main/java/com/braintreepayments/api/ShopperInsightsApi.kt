package com.braintreepayments.api

/**
 * [ShopperInsightsApi] is a wrapper class for all api related classes that
 * [ShopperInsightsClient] depends on.
 */
internal class ShopperInsightsApi(
    private val eligiblePaymentsApi: EligiblePaymentsApi
) {

    fun findEligiblePayments(request: EligiblePaymentsApiRequest): EligiblePaymentsApiResult {
        return eligiblePaymentsApi.execute(request)
    }
}
