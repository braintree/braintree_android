package com.braintreepayments.api.findeligiblepayments

import com.braintreepayments.api.findeligiblepayments.FindEligiblePaymentsApiRequest.Companion.toJson

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
