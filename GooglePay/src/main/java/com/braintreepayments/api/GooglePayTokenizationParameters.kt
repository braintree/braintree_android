package com.braintreepayments.api

import com.google.android.gms.wallet.PaymentMethodTokenizationParameters

/**
 * A request used to launch the Venmo app for continuation of the Venmo payment flow.
 */
sealed class GooglePayTokenizationParameters {

    /**
     * The request was successfully created and is ready to be launched by [VenmoLauncher]
     */
    class Success(val parameters: PaymentMethodTokenizationParameters, val allowedCardNetworks:
    Collection<Integer>) : GooglePayTokenizationParameters()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : GooglePayTokenizationParameters()
}
