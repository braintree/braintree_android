package com.braintreepayments.api.visacheckout

import com.visa.checkout.Profile

/**
 * Result of creating a Visa Checkout profile builder
 */
sealed class VisaCheckoutProfileBuilderResult {

    /**
     * The [profileBuilder] was successfully created
     */
    class Success internal constructor(
        val profileBuilder: Profile.ProfileBuilder
    ) : VisaCheckoutProfileBuilderResult()

    /**
     * There was an [error] creating the profile builder
     */
    class Failure internal constructor(val error: Exception) : VisaCheckoutProfileBuilderResult()
}
