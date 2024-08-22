package com.braintreepayments.api.threedsecure

import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse

/**
 * Result returned from [ThreeDSecureLauncher.launch]
 *
 * @property jwt jwt value
 * @property validateResponse validate response data from Cardinal
 * @property threeDSecureParams additional parameters from the auth request
 * @property error error if the auth request failed
 */
data class ThreeDSecurePaymentAuthResult internal constructor(
    val jwt: String? = null,
    val validateResponse: ValidateResponse? = null,
    val threeDSecureParams: ThreeDSecureParams? = null,
    val error: Exception? = null
)
