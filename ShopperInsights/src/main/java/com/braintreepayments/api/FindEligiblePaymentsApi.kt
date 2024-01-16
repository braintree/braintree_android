package com.braintreepayments.api

import com.braintreepayments.api.FindEligiblePaymentsApiRequest.Companion.toJson

internal class FindEligiblePaymentsApi {
    fun execute(request: FindEligiblePaymentsApiRequest): FindEligiblePaymentsApiResult {
        request.toJson()
        // TODO: Network call

        // Hardcoded result
        return FindEligiblePaymentsApiResult(
            eligibleMethods = FindEligiblePaymentMethods(
                paypal = FindEligiblePaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPayPalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                ),
                venmo = FindEligiblePaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPayPalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                )
            )
        )
    }
}
