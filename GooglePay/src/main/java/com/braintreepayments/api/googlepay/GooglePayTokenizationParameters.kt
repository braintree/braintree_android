package com.braintreepayments.api.googlepay

import com.google.android.gms.wallet.PaymentMethodTokenizationParameters

/**
 * A request used to get Braintree specific tokenization parameters for a Google Pay
 */
sealed class GooglePayTokenizationParameters {

    /**
     * The request was successfully created
     */
    class Success(val parameters: PaymentMethodTokenizationParameters,
                  val allowedCardNetworks: Collection<Integer>
    ) : GooglePayTokenizationParameters()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : GooglePayTokenizationParameters()
}
