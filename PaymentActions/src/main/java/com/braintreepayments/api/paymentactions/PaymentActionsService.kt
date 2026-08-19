package com.braintreepayments.api.paymentactions

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
internal class PaymentActionsService(
    private val braintreeClient: BraintreeClient,
) {

    /**
     * Submits a payment method to `setPaymentActionPaymentMethod` GraphQL mutation.
     *
     * On success returns a [PaymentActionServiceResult.Success] wrapping the raw [PaymentAction].
     * On failure returns a [PaymentActionServiceResult.Failure] wrapping an [Exception].
     *
     * @param paymentMethod the [PaymentActionRequest] to submit to GraphQL.
     * @return a [PaymentActionServiceResult] wrapper denoting success or failure.
     */
    suspend fun setPaymentActionPaymentMethod(
        paymentMethod: PaymentActionRequest,
    ): PaymentActionServiceResult {
        return try {
            braintreeClient
                .sendGraphQLPOST(buildSetPaymentActionPaymentMethodQuery(paymentMethod))
                .toPaymentActionServiceResult()
        } catch (exception: IOException) {
            PaymentActionServiceResult.Failure(exception)
        }
    }

    /**
     * Fetches the current state of a payment action.
     *
     * Stubbed for now — the GraphQL query for fetching a payment action by id is not yet defined.
     */
    suspend fun getPaymentAction(): PaymentActionServiceResult {
        throw NotImplementedError("getPaymentAction is not yet implemented")
    }

    private fun buildSetPaymentActionPaymentMethodQuery(paymentMethod: PaymentActionRequest): JSONObject {
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

    private fun String.toPaymentActionServiceResult(): PaymentActionServiceResult {
        return try {
            val responseJson = JSONObject(this)
            if (responseJson.has(GraphQLConstants.Keys.ERRORS) &&
                responseJson.getJSONArray(GraphQLConstants.Keys.ERRORS).length() > 0
            ) {
                return PaymentActionServiceResult.Failure(BraintreeException(responseJson.toString()))
            }

            responseJson
                .getJSONObject(DATA_KEY)
                .getJSONObject(SET_PAYMENT_ACTION_PAYMENT_METHOD_KEY)
                .getJSONObject(PAYMENT_ACTION_KEY)
                .let { paymentActionJson ->
                    PaymentActionServiceResult.Success(
                        PaymentAction(
                            id = paymentActionJson.getString(ID_KEY),
                            status = paymentActionJson.getString(STATUS_KEY).toPaymentActionStatus()
                        )
                    )
                }
        } catch (jex: JSONException) {
            PaymentActionServiceResult.Failure(jex)
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
