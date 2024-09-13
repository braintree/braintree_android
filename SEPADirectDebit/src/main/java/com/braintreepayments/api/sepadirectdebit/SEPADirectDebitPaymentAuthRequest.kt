package com.braintreepayments.api.sepadirectdebit

/**
 * A request used to launch the continuation of the SEPA Direct Debit flow.
 */
sealed class SEPADirectDebitPaymentAuthRequest {

    // TODO: make requestParams internal when SEPADirectDebitClientUnitTest is converted to Kotlin
    /**
     * The request was successfully created and is ready to be launched by [SEPADirectDebitLauncher]
     * @param requestParams this parameter is intended for internal use only. It is not covered by
     * semantic versioning and may be changed or removed at any time.
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
