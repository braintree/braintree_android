package com.braintreepayments.api.sepadirectdebit

data class CreateMandateResult internal constructor(
    val approvalUrl: String,
    val ibanLastFour: String,
    val customerId: String,
    val bankReferenceToken: String,
    val mandateType: SEPADirectDebitMandateType
)
