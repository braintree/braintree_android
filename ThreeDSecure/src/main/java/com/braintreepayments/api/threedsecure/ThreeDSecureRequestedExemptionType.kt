package com.braintreepayments.api.threedsecure

enum class ThreeDSecureRequestedExemptionType(val stringValue: String) {
    LOW_VALUE("low_value"),
    SECURE_CORPORATE("secure_corporate"),
    TRUSTED_BENEFICIARY("trusted_beneficiary"),
    TRANSACTION_RISK_ANALYSIS("transaction_risk_analysis")
}
