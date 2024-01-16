package com.braintreepayments.api

import com.braintreepayments.api.findeligiblepayments.FindEligiblePaymentsApiResult
import com.braintreepayments.api.findeligiblepayments.FindEligiblePaymentsApi
import com.braintreepayments.api.findeligiblepayments.FindEligiblePaymentsApiRequest

/**
 * [ShopperInsightsApi] is a wrapper class for all api related classes that
 * [ShopperInsightsClient] depends on.
 */
internal class ShopperInsightsApi(
    private val paymentsApi: FindEligiblePaymentsApi
) {

    fun findEligiblePayments(request: FindEligiblePaymentsApiRequest): FindEligiblePaymentsApiResult {
        return paymentsApi.execute(request)
    }
}
