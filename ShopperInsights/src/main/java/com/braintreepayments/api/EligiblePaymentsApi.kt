package com.braintreepayments.api

import com.braintreepayments.api.EligiblePaymentsApiRequest.Companion.toJson

internal class EligiblePaymentsApi {
    fun execute(request: EligiblePaymentsApiRequest): EligiblePaymentsApiResult {
        request.toJson()
        // TODO: Network call

        // Hardcoded result
        return EligiblePaymentsApiResult(
            eligibleMethods = EligiblePaymentMethods(
                paypal = EligiblePaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPayPalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                ),
                venmo = EligiblePaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPayPalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                )
            )
        )
    }
}
