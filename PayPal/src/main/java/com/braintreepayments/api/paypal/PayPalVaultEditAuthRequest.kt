package com.braintreepayments.api.paypal

sealed class PayPalVaultEditAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [PayPalLauncher]
     */

    //TODO: update PayPalPaymentAuthRequestParams
    class ReadyToLaunch(val requestParams: PayPalPaymentAuthRequestParams) :
        PayPalVaultEditAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : PayPalVaultEditAuthRequest()
}