package com.braintreepayments.api.shopperinsights

enum class ExperimentType(val rawValue: String) {
    CONTROL("control"),
    TEST("test");

    fun formattedExperiment(): String {
        return """
            [
                { "exp_name" : "PaymentReady" }
                { "treatment_name" : $rawValue }
            ]
        """
    }
}
