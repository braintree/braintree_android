package com.braintreepayments.api.sepadirectdebit

/**
 * A pending request for the SEPA Direct Debit web-based authentication flow created by invoking
 * [SEPADirectDebitLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [SEPADirectDebitLauncher.handleReturnToApp]
 */
sealed class SEPADirectDebitPendingRequest {

    /**
     * A pending request was successfully started.
     *
     * @property pendingRequestString - This String should be stored and passed to
     * [SEPADirectDebitLauncher.handleReturnToApp].
     */
    class Started internal constructor(val pendingRequestString: String) : SEPADirectDebitPendingRequest()

    /**
     * An error occurred launching the SEPA Direct Debit browser flow. See [error] for details.
     */
    class Failure internal constructor(val error: Exception) : SEPADirectDebitPendingRequest()
}
