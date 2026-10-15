package com.braintreepayments.api.paymentactions

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

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
                .toPaymentActionServiceResult { data ->
                    data.getJSONObject(SET_PAYMENT_ACTION_PAYMENT_METHOD_KEY).getJSONObject(PAYMENT_ACTION_KEY)
                }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            PaymentActionServiceResult.Failure(exception)
        }
    }

    /**
     * Self-fetches the current state of the Payment Action scoped to this client's authorization.
     *
     * On success returns a [PaymentActionServiceResult.Success] wrapping the raw [PaymentAction].
     * On failure returns a [PaymentActionServiceResult.Failure] wrapping an [Exception].
     *
     * @return a [PaymentActionServiceResult] wrapper denoting success or failure.
     */
    suspend fun getPaymentAction(): PaymentActionServiceResult {
        return try {
            braintreeClient
                .sendGraphQLPOST(buildGetPaymentActionQuery())
                .toPaymentActionServiceResult { data -> data.getJSONObject(PAYMENT_ACTION_KEY) }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            PaymentActionServiceResult.Failure(exception)
        }
    }

    /**
     * Signals that the customer has completed the current `nextAction` (e.g. device data collection
     * or a 3D Secure challenge), advancing the Payment Action to its next state.
     *
     * On success returns a [PaymentActionServiceResult.Success] wrapping the raw [PaymentAction],
     * which may carry a new `nextAction` if another customer action is required.
     * On failure returns a [PaymentActionServiceResult.Failure] wrapping an [Exception].
     *
     * @param completedAction the completed action's payload — mirrors the GraphQL `@oneOf` input.
     * @return a [PaymentActionServiceResult] wrapper denoting success or failure.
     */
    suspend fun signalCustomerActionComplete(
        completedAction: CustomerCompletedAction,
    ): PaymentActionServiceResult {
        return try {
            braintreeClient
                .sendGraphQLPOST(buildSignalCustomerActionCompleteQuery(completedAction))
                .toPaymentActionServiceResult { data ->
                    data.getJSONObject(SIGNAL_CUSTOMER_ACTION_COMPLETE_KEY).getJSONObject(PAYMENT_ACTION_KEY)
                }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            PaymentActionServiceResult.Failure(exception)
        }
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

    /**
     * Builds the GraphQL request body for the `GetPaymentAction` query, which self-fetches the
     * current Payment Action using the request's auth fingerprint rather than an explicit id.
     */
    private fun buildGetPaymentActionQuery(): JSONObject {
        val query = """
            query GetPaymentAction {
                paymentAction {
                    ${PaymentActionsGraphQLConstants.PAYMENT_ACTION_SELECTION_SET.trimIndent()}
                }
            }
        """.trimIndent()

        return JSONObject()
            .put(GraphQLConstants.Keys.QUERY, query)
            .put(GraphQLConstants.Keys.VARIABLES, JSONObject())
    }

    /**
     * Builds the GraphQL request body for the `SignalCustomerActionComplete` mutation. Omits
     * `paymentActionId`, since the server resolves the Payment Action from the request's auth
     * fingerprint.
     */
    private fun buildSignalCustomerActionCompleteQuery(completedAction: CustomerCompletedAction): JSONObject {
        val input = JSONObject().put(
            PaymentActionsGraphQLConstants.Keys.CUSTOMER_ACTION_COMPLETE,
            completedAction.toGraphQLVariables(),
        )
        val variables = JSONObject().put(GraphQLConstants.Keys.INPUT, input)

        val mutation = """
            mutation SignalCustomerActionComplete(${'$'}input: SignalCustomerActionCompleteInput!) {
                signalCustomerActionComplete(input: ${'$'}input) {
                    paymentAction {
                        ${PaymentActionsGraphQLConstants.PAYMENT_ACTION_SELECTION_SET.trimIndent()}
                    }
                }
            }
        """.trimIndent()

        return JSONObject()
            .put(GraphQLConstants.Keys.QUERY, mutation)
            .put(GraphQLConstants.Keys.VARIABLES, variables)
    }

    /**
     * Parses a `data` -> ... -> `paymentAction` GraphQL response shared by `setPaymentActionPaymentMethod`,
     * `getPaymentAction`, and `signalCustomerActionComplete`.
     *
     * @param extractPaymentAction navigates from the top-level `data` object to the operation-specific
     *   `paymentAction` object, since each operation nests it under a different parent key.
     */
    private fun String.toPaymentActionServiceResult(
        extractPaymentAction: (JSONObject) -> JSONObject,
    ): PaymentActionServiceResult {
        return try {
            val responseJson = JSONObject(this)
            if (responseJson.has(GraphQLConstants.Keys.ERRORS) &&
                responseJson.getJSONArray(GraphQLConstants.Keys.ERRORS).length() > 0
            ) {
                return PaymentActionServiceResult.Failure(BraintreeException(responseJson.toString()))
            }

            val paymentActionJson = extractPaymentAction(responseJson.getJSONObject(DATA_KEY))
            PaymentActionServiceResult.Success(paymentActionJson.toPaymentAction())
        } catch (jex: JSONException) {
            PaymentActionServiceResult.Failure(jex)
        }
    }

    private fun JSONObject.toPaymentAction(): PaymentAction {
        return PaymentAction(
            id = getString(ID_KEY),
            status = getString(STATUS_KEY).toPaymentActionStatus(),
            nextAction = optJSONObject(NEXT_ACTION_KEY)?.toPaymentActionNextAction(),
        )
    }

    private fun JSONObject.toPaymentActionNextAction(): PaymentActionNextAction {
        val typeName = optString(TYPENAME_KEY).takeIf { it.isNotEmpty() }
        return when (typeName) {
            PaymentActionsGraphQLConstants.TypeNames.THREE_D_SECURE_AUTHENTICATION ->
                PaymentActionNextAction.ThreeDSecureAuthentication(bin = getString(BIN_KEY))
            PaymentActionsGraphQLConstants.TypeNames.THREE_D_SECURE_CHALLENGE ->
                PaymentActionNextAction.ThreeDSecureChallenge(
                    acsUrl = getString(ACS_URL_KEY),
                    acsRenderingType = optString(ACS_RENDERING_TYPE_KEY).takeIf { it.isNotEmpty() },
                    challengePayload = getString(CHALLENGE_PAYLOAD_KEY),
                )
            else -> PaymentActionNextAction.Unrecognized(typeName)
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
        private const val SIGNAL_CUSTOMER_ACTION_COMPLETE_KEY = "signalCustomerActionComplete"
        private const val PAYMENT_ACTION_KEY = "paymentAction"
        private const val ID_KEY = "id"
        private const val STATUS_KEY = "status"
        private const val NEXT_ACTION_KEY = "nextAction"
        private const val TYPENAME_KEY = "__typename"
        private const val BIN_KEY = "bin"
        private const val ACS_URL_KEY = "acsUrl"
        private const val ACS_RENDERING_TYPE_KEY = "acsRenderingType"
        private const val CHALLENGE_PAYLOAD_KEY = "challengePayload"
    }
}
