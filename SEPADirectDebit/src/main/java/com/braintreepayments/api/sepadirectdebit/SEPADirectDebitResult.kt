package com.braintreepayments.api.sepadirectdebit

/**
 * Result of tokenizing a SEPA Direct Debit payment method
 */
sealed class SEPADirectDebitResult {

    /**
     * The SEPA Direct Debit flow completed successfully. This [nonce] should be sent to
     * your server.
     */
    class Success(val nonce: SEPADirectDebitNonce) : SEPADirectDebitResult()

    /**
     * There was an [error] in the SEPA Direct Debit flow.
     */
    class Failure(val error: Exception) : SEPADirectDebitResult()

    /**
     * The user canceled the SEPA Direct Debit flow.
     */
    data object Cancel : SEPADirectDebitResult()
}
