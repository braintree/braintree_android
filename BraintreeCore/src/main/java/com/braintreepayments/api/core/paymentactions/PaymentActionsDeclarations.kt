package com.braintreepayments.api.core.paymentactions

import androidx.annotation.RestrictTo
import org.json.JSONObject

/**
 * Defines a payment method that can be used with [PaymentActionsService].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface PaymentActionPaymentMethod {
    /**
     * Produces the set of GraphQL variables needed to satisfy the GraphQL call in
     * [PaymentActionsService.setPaymentActionPaymentMethod].
     */
    fun toGraphQLVariables(): JSONObject

    /**
     * A string representing the minimal selection set for the GraphQL call in
     * [PaymentActionsService.setPaymentActionPaymentMethod].
     */
    fun paymentActionSelectionSet(): String = """
        id
        status
    """.trimIndent()
}

/**
 * Wrapper result type for [PaymentAction].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentActionResult {
    class Success(val paymentAction: PaymentAction) : PaymentActionResult()
    class Failure(val error: Exception) : PaymentActionResult()
}

/**
 * Type that models the shape of a payment action response.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentAction(
    val id: String,
    val status: PaymentActionStatus,
)

/**
 * Enum of possible payment action status values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentActionStatus {
    SUCCEEDED,
    REQUIRES_CAPTURE,
    REQUIRES_PAYMENT_METHOD,
    UNKNOWN
}
