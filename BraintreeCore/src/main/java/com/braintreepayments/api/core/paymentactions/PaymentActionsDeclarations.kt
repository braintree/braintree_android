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
     * A string representing the selection set for the GraphQL call in
     * [PaymentActionsService.setPaymentActionPaymentMethod].
     *
     * `selectedPaymentMethod.details` is omitted because it's a union type. Override this
     * function when the response must include `selectedPaymentMethod.details`.
     *
     * Known `selectedPaymentMethod.details` types:
     * - `CreditCardDetails`
     */
    fun paymentActionSelectionSet(): String = """
        id
        status
        nextAction {
            type
            cardinalAuthenticationJwt
            bin
            acsUrl
            challengePayload
            redirectUrl
        }
        selectedPaymentMethod {
            paymentMethodId
            usage
        }
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
 * DTO reflecting the shape of a payment action response.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentAction(
    val id: String,
    val status: PaymentActionStatus,
    val nextAction: PaymentActionNextAction?,
    val selectedPaymentMethod: PaymentActionSelectedPaymentMethod?,
)

/**
 * DTO reflecting the shape of a payment action's selected payment method, as returned by the
 * `selectedPaymentMethod` field on `PaymentAction`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentActionSelectedPaymentMethod(
    val paymentMethodId: String?,
    val usage: String?,
    val details: PaymentActionSelectedPaymentMethodDetails?,
)

/**
 * A sealed class that represents the different shapes `selectedPaymentMethod.details` can take
 * when returned from GraphQL.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentActionSelectedPaymentMethodDetails {
    data class CreditCard(
        val last4: String,
        val bin: String,
        val expirationMonth: String,
        val expirationYear: String,
        val cardholderName: String,
        val brandCode: String,
    ) : PaymentActionSelectedPaymentMethodDetails()

    data object Unknown : PaymentActionSelectedPaymentMethodDetails()
}

/**
 * Enum of possible payment action status values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentActionStatus {
    SUCCEEDED,
    PROCESSING,
    REQUIRES_CAPTURE,
    READY_FOR_CONFIRMATION,
    REQUIRES_PAYMENT_METHOD,
    REQUIRES_CUSTOMER_ACTION,
    CANCELED,
    EXPIRED,
    UNKNOWN
}

/**
 * A sealed class that represents the different shapes a Next Action can take when returned from
 * GraphQL.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentActionNextAction {
    data object ProvideCvv : PaymentActionNextAction()

    data class Redirect(val redirectUrl: String) : PaymentActionNextAction()

    data class ThreeDSecure(
        val songbirdUrl: String?,
        val cardinalAuthenticationJwt: String?,
        val bin: String?,
        val acsUrl: String?,
        val challengePayload: String?,
    ) : PaymentActionNextAction()

    data object Unknown : PaymentActionNextAction()
}

/**
 * Payment Actions Analytics Keys.
 *
 * TODO: These keys are currently placeholder values that must be changed when we get the official values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentActionsAnalytics {
    const val SET_PAYMENT_METHOD_STARTED = "payment-actions:set-payment-method:started"
    const val SET_PAYMENT_METHOD_SUCCEEDED = "payment-actions:set-payment-method:succeeded"
    const val SET_PAYMENT_METHOD_FAILED = "payment-actions:set-payment-method:failed"
    const val READY_FOR_CONFIRMATION = "payment-actions:ready-for-confirmation"
}
