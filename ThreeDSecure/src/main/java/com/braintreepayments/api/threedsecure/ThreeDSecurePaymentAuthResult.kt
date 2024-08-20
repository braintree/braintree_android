package com.braintreepayments.api.threedsecure

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse

data class ThreeDSecurePaymentAuthResult internal constructor(
    val jwt: String? = null,
    val validateResponse: ValidateResponse? = null,
    val threeDSecureParams: ThreeDSecureParams? = null,
    val error: Exception? = null
)
