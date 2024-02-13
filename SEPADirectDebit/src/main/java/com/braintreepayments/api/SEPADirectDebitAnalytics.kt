package com.braintreepayments.api

internal object SEPADirectDebitAnalytics {

    // Conversion Events
    const val TOKENIZE_STARTED = "sepa:tokenize:started"
    const val TOKENIZE_SUCCEEDED = "sepa:tokenize:succeeded"
    const val TOKENIZE_FAILED = "sepa:tokenize:failed"
    const val CHALLENGE_CANCELED = "sepa:tokenize:challenge:canceled"

    // Additional Detail Events
    const val CREATE_MANDATE_CHALLENGE_REQUIRED = "sepa:tokenize:create-mandate:challenge-required"
    const val CREATE_MANDATE_SUCCEEDED = "sepa:tokenize:create-mandate:succeeded"
    const val CREATE_MANDATE_FAILED = "sepa:tokenize:create-mandate:failed"
    const val CHALLENGE_PRESENTATION_SUCCEEDED = "sepa:tokenize:challenge-presentation:succeeded"
    const val CHALLENGE_PRESENTATION_FAILED = "sepa:tokenize:challenge-presentation:failed"
    const val CHALLENGE_SUCCEEDED = "sepa:tokenize:challenge:succeeded"
    const val CHALLENGE_FAILED = "sepa:tokenize:challenge:failed"
}
