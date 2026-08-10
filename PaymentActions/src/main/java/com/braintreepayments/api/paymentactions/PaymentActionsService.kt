package com.braintreepayments.api.paymentactions

import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * This is a centralized service for handling all Payment Actions related backend requests.
 * It acts as a thin client in order to insulate our payment method specific clients from the
 * networking layer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentActionsService(
    private val braintreeClient: BraintreeClient,
) {

    /**
     * Submits a payment method to `setPaymentActionPaymentMethod` GraphQL mutation.
     *
     * On success returns a [PaymentActionResult.Success] wrapping a [PaymentAction] is returned.
     * On failure returns a [PaymentActionResult.Failure] wrapping an [Exception] is returned.
     *
     * @param paymentMethod the [PaymentActionPaymentMethod] to submit to GraphQL.
     * @return a [PaymentActionResult] wrapper denoting success or failure.
     */
    suspend fun setPaymentActionPaymentMethod(
        paymentMethod: PaymentActionPaymentMethod,
    ): PaymentActionResult {
        return try {
            braintreeClient
                .sendGraphQLPOST(buildSetPaymentActionPaymentMethodQuery(paymentMethod))
                .toPaymentActionsResult()
        } catch (exception: IOException) {
            PaymentActionResult.Failure(exception)
        }
    }

    private fun buildSetPaymentActionPaymentMethodQuery(paymentMethod: PaymentActionPaymentMethod): JSONObject {
        val input = JSONObject().put(PAYMENT_METHOD_KEY, paymentMethod.toGraphQLVariables())
        val variables = JSONObject().put(GraphQLConstants.Keys.INPUT, input)

        val mutation = """
            mutation SetPaymentActionPaymentMethod(${'$'}input: SetPaymentActionPaymentMethodInput!) {
                setPaymentActionPaymentMethod(input: ${'$'}input) {
                    paymentAction {
                        ${paymentMethod.paymentActionSelectionSet()}
                    }
                }
            }
        """.trimIndent()

        return JSONObject()
            .put(GraphQLConstants.Keys.QUERY, mutation)
            .put(GraphQLConstants.Keys.VARIABLES, variables)
    }

    private fun String.toPaymentActionsResult(): PaymentActionResult {
        return try {
            val responseJson = JSONObject(this)
            if (responseJson.has(GraphQLConstants.Keys.ERRORS) &&
                responseJson.getJSONArray(GraphQLConstants.Keys.ERRORS).length() > 0
            ) {
                return PaymentActionResult.Failure(BraintreeException(responseJson.toString()))
            }

            responseJson
                .getJSONObject(DATA_KEY)
                .getJSONObject(SET_PAYMENT_ACTION_PAYMENT_METHOD_KEY)
                .getJSONObject(PAYMENT_ACTION_KEY)
                .let { paymentActionJson ->
                    PaymentAction(
                        id = paymentActionJson.getString(ID_KEY),
                        status = paymentActionJson.getString(STATUS_KEY).toPaymentActionStatus()
                    ).let { paymentAction ->
                        PaymentActionResult.Success(paymentAction)
                    }
                }
        } catch (jex: JSONException) {
            PaymentActionResult.Failure(jex)
        }
    }

    private fun String.toPaymentActionStatus(): PaymentActionStatus {
        return try {
            PaymentActionStatus.valueOf(this.uppercase())
        } catch (_: IllegalArgumentException) {
            PaymentActionStatus.UNKNOWN
        }
    }

    companion object {
        private const val PAYMENT_METHOD_KEY = "paymentMethod"
        private const val DATA_KEY = "data"
        private const val SET_PAYMENT_ACTION_PAYMENT_METHOD_KEY = "setPaymentActionPaymentMethod"
        private const val PAYMENT_ACTION_KEY = "paymentAction"
        private const val ID_KEY = "id"
        private const val STATUS_KEY = "status"
    }
}
