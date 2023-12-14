package com.braintreepayments.api

/**
 * A request used to launch the continuation of the SEPA Direct Debit flow.
 */
sealed class SEPADirectDebitPaymentAuthRequest {

    /**
     * The request was successfully created and is ready to be launched by [SEPADirectDebitLauncher]
     */
    class ReadyToLaunch(val requestParams: SEPADirectDebitPaymentAuthRequestParams) :
        SEPADirectDebitPaymentAuthRequest()

    /**
     * No web-based mandate is required. Send this [nonce] to your server
     */
    class LaunchNotRequired(val nonce: SEPADirectDebitNonce) : SEPADirectDebitPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : SEPADirectDebitPaymentAuthRequest()
}
