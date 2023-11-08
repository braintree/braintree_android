package com.braintreepayments.api

internal enum class SEPADirectDebitAnalytics(@JvmField val event: String) {

    // Conversion Events
    TOKENIZE_STARTED("sepa:tokenize:started"),
    TOKENIZE_SUCCEEDED("sepa:tokenize:succeeded"),
    TOKENIZE_FAILED("sepa:tokenize:failed"),
    CHALLENGE_CANCELED("sepa:tokenize:challenge:canceled"),

    // Additional Detail Events
    CREATE_MANDATE_CHALLENGE_REQUIRED("sepa:tokenize:create-mandate:challenge-required"),
    CREATE_MANDATE_SUCCEEDED("sepa:tokenize:create-mandate:succeeded"),
    CREATE_MANDATE_FAILED("sepa:tokenize:create-mandate:failed"),
    CHALLENGE_PRESENTATION_SUCCEEDED("sepa:tokenize:challenge-presentation:succeeded"),
    CHALLENGE_PRESENTATION_FAILED("sepa:tokenize:challenge-presentation:failed"),
    CHALLENGE_SUCCEEDED("sepa:tokenize:challenge:succeeded"),
    CHALLENGE_FAILED("sepa:tokenize:challenge:failed")
}
