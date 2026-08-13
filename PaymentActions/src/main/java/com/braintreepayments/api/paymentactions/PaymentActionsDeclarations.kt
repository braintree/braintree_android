package com.braintreepayments.api.paymentactions

import org.json.JSONObject

/**
 * Defines a payment method that can be used with [PaymentActionsService].
 */
sealed class PaymentActionRequest {

    /**
     * Produces the set of GraphQL variables needed to satisfy the GraphQL call in
     * [PaymentActionsService.setPaymentActionPaymentMethod].
     */
    internal abstract fun toGraphQLVariables(): JSONObject

    /**
     * A string representing the minimal selection set for the GraphQL call in
     * [PaymentActionsService.setPaymentActionPaymentMethod].
     */
    internal open fun paymentActionSelectionSet(): String = """
        id
        status
    """.trimIndent()
}

/**
 * Unvaulted credit card input for a payment action.
 */
class CreditCard(
    val number: String,
    val expirationMonth: String,
    val expirationYear: String,
    val cvv: String? = null,
    val cardholderName: String? = null,
    val streetAddress: String? = null,
    val extendedAddress: String? = null,
    val locality: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val countryCodeAlpha2: String? = null,
) : PaymentActionRequest() {

    override fun toGraphQLVariables(): JSONObject {
        return JSONObject().apply {
            streetAddress?.let { put(PaymentActionsGraphQLConstants.Keys.BILLING_ADDRESS_STREET_ADDRESS, it) }
            extendedAddress?.let { put(PaymentActionsGraphQLConstants.Keys.BILLING_ADDRESS_EXTENDED_ADDRESS, it) }
            locality?.let { put(PaymentActionsGraphQLConstants.Keys.BILLING_ADDRESS_LOCALITY, it) }
            region?.let { put(PaymentActionsGraphQLConstants.Keys.BILLING_ADDRESS_REGION, it) }
            postalCode?.let { put(PaymentActionsGraphQLConstants.Keys.BILLING_ADDRESS_POSTAL_CODE, it) }
            countryCodeAlpha2?.let {
                put(PaymentActionsGraphQLConstants.Keys.BILLING_ADDRESS_COUNTRY_CODE_ALPHA_2, it)
            }
        }.let { billingAddressFields ->
            JSONObject().apply {
                put(PaymentActionsGraphQLConstants.Keys.CREDIT_CARD_NUMBER, number)
                put(PaymentActionsGraphQLConstants.Keys.CREDIT_CARD_EXPIRATION_MONTH, expirationMonth)
                put(PaymentActionsGraphQLConstants.Keys.CREDIT_CARD_EXPIRATION_YEAR, expirationYear)
                cvv?.let { put(PaymentActionsGraphQLConstants.Keys.CREDIT_CARD_CVV, it) }
                cardholderName?.let { put(PaymentActionsGraphQLConstants.Keys.CREDIT_CARD_CARDHOLDER_NAME, it) }
                if (billingAddressFields.length() > 0) {
                    put(PaymentActionsGraphQLConstants.Keys.CREDIT_CARD_BILLING_ADDRESS, billingAddressFields)
                }
            }
        }.let { creditCardFields ->
            JSONObject().put(PaymentActionsGraphQLConstants.Keys.CREDIT_CARD, creditCardFields)
        }.let { paymentMethodDetails ->
            JSONObject().put(PaymentActionsGraphQLConstants.Keys.PAYMENT_METHOD_DETAILS, paymentMethodDetails)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CreditCard) return false
        return number == other.number &&
            expirationMonth == other.expirationMonth &&
            expirationYear == other.expirationYear &&
            cvv == other.cvv &&
            cardholderName == other.cardholderName &&
            streetAddress == other.streetAddress &&
            extendedAddress == other.extendedAddress &&
            locality == other.locality &&
            region == other.region &&
            postalCode == other.postalCode &&
            countryCodeAlpha2 == other.countryCodeAlpha2
    }

    override fun hashCode(): Int {
        var result = number.hashCode()
        result = 31 * result + expirationMonth.hashCode()
        result = 31 * result + expirationYear.hashCode()
        result = 31 * result + (cvv?.hashCode() ?: 0)
        result = 31 * result + (cardholderName?.hashCode() ?: 0)
        result = 31 * result + (streetAddress?.hashCode() ?: 0)
        result = 31 * result + (extendedAddress?.hashCode() ?: 0)
        result = 31 * result + (locality?.hashCode() ?: 0)
        result = 31 * result + (region?.hashCode() ?: 0)
        result = 31 * result + (postalCode?.hashCode() ?: 0)
        result = 31 * result + (countryCodeAlpha2?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "CreditCard(number='$number', expirationMonth='$expirationMonth', " +
            "expirationYear='$expirationYear', cvv=$cvv, cardholderName=$cardholderName, " +
            "streetAddress=$streetAddress, extendedAddress=$extendedAddress, locality=$locality, " +
            "region=$region, postalCode=$postalCode, countryCodeAlpha2=$countryCodeAlpha2)"
    }
}

/**
 * Type that models the shape of a payment action response.
 */
internal data class PaymentAction(
    val id: String,
    val status: PaymentActionStatus,
)

/**
 * Enum of possible payment action status values.
 */
internal enum class PaymentActionStatus {
    CANCELED,
    EXPIRED,
    PROCESSING,
    READY_FOR_CONFIRMATION,
    REQUIRES_CAPTURE,
    REQUIRES_CUSTOMER_ACTION,
    REQUIRES_PAYMENT_METHOD,
    SUCCEEDED,
    UNKNOWN,
}

/**
 * Merchant gateway configuration for whether a payment action is confirmed automatically by the
 * server or requires an explicit confirmation step.
 */
internal enum class ConfirmationMethod {
    AUTOMATIC,
    MANUAL,
}

/**
 * Merchant gateway configuration for whether a payment action is captured automatically by the
 * server or requires an explicit capture step.
 */
internal enum class CaptureMethod {
    AUTOMATIC,
    MANUAL,
}

/**
 * Wrapper result type for [PaymentActionsService], carrying the raw [PaymentAction]
 * returned by GraphQL.
 */
internal sealed class PaymentActionServiceResult {
    class Success(val paymentAction: PaymentAction) : PaymentActionServiceResult()
    class Failure(val error: Exception) : PaymentActionServiceResult()
}

/**
 * The result of a Payment Action operation.
 */
sealed class PaymentActionResult {
    class Completed(val id: String) : PaymentActionResult()
    class Canceled(val id: String) : PaymentActionResult()
    class ServerActionRequired(
        val id: String,
        val serverAction: ServerAction,
    ) : PaymentActionResult()

    class PaymentMethodRequired(val id: String) : PaymentActionResult()
    class CustomerActionRequired(val id: String, ) : PaymentActionResult()
    class Failure(val error: Throwable) : PaymentActionResult()
    class Processing(val id: String): PaymentActionResult()
}

/**
 * A server-driven action the merchant/SDK must perform to advance a payment action.
 */
enum class ServerAction {
    CONFIRM,
    CAPTURE,
}
