package com.braintreepayments.api.shopperinsights

/**
 * An ExperimentType that is either a control or test type.
 */
enum class ExperimentType(private val rawValue: String) {
    CONTROL("control"),
    TEST("test");

    internal fun formattedExperiment(): String {
        return """
            [
                { "exp_name" : "PaymentReady" }
                { "treatment_name" : $rawValue }
            ]
        """
    }
}
