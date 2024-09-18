package com.braintreepayments.api.googlepay

import com.google.android.gms.wallet.PaymentMethodTokenizationParameters

/**
 * A request used to get Braintree specific tokenization parameters for a Google Pay
 */
sealed class GooglePayTokenizationParameters {

    /**
     * The request was successfully created
     */
    class Success internal constructor(
        val parameters: PaymentMethodTokenizationParameters,
        val allowedCardNetworks: Collection<Int>
    ) : GooglePayTokenizationParameters()

    /**
     * There was an [error] creating the request
     */
    class Failure internal constructor(val error: Exception) : GooglePayTokenizationParameters()
}
