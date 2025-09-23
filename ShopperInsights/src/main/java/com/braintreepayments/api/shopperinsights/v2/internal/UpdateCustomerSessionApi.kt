package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import org.json.JSONException
import org.json.JSONObject

/**
 * API to update an existing customer session using the `UpdateCustomerSession` GraphQL mutation.
 */
@ExperimentalBetaApi
internal class UpdateCustomerSessionApi(
    private val braintreeClient: BraintreeClient,
    private val customerSessionRequestBuilder: CustomerSessionRequestBuilder = CustomerSessionRequestBuilder(),
    private val responseParser: ShopperInsightsResponseParser = ShopperInsightsResponseParser()
) {

    sealed class UpdateCustomerSessionResult {
        data class Success(val sessionId: String) : UpdateCustomerSessionResult()
        data class Error(val error: Exception) : UpdateCustomerSessionResult()
    }

    fun execute(
        customerSessionRequest: CustomerSessionRequest,
        sessionId: String,
        callback: (UpdateCustomerSessionResult) -> Unit
    ) {
        try {
            val params = JSONObject()
            params.put(
                QUERY, """
                mutation UpdateCustomerSession(${'$'}input: UpdateCustomerSessionInput!) {
                    updateCustomerSession(input: ${'$'}input) {
                        sessionId
                    }
                }
                """.trimIndent()
            )

            params.put(VARIABLES, assembleVariables(sessionId, customerSessionRequest))

            braintreeClient.sendGraphQLPOST(params) { responseBody: String?, httpError: Exception? ->
                if (responseBody != null) {
                    val sessionId = responseParser.parseSessionId(responseBody, UPDATE_CUSTOMER_SESSION)
                    callback(UpdateCustomerSessionResult.Success(sessionId))
                } else if (httpError != null) {
                    callback(UpdateCustomerSessionResult.Error(httpError))
                }
            }
        } catch (e: JSONException) {
            callback(UpdateCustomerSessionResult.Error(e))
        }
    }

    @Throws(JSONException::class)
    private fun assembleVariables(
        sessionId: String,
        customerSessionRequest: CustomerSessionRequest
    ): JSONObject {
        val jsonRequestObjects = customerSessionRequestBuilder.createRequestObjects(customerSessionRequest)

        val input = JSONObject().apply {
            put(SESSION_ID, sessionId)
            put(CUSTOMER, jsonRequestObjects.customer)
            putOpt(PURCHASE_UNITS, jsonRequestObjects.purchaseUnits)
        }

        return JSONObject().put(INPUT, input)
    }

    companion object {
        private const val QUERY = "query"
        private const val VARIABLES = "variables"
        private const val INPUT = "input"
        private const val SESSION_ID = "sessionId"
        private const val CUSTOMER = "customer"
        private const val PURCHASE_UNITS = "purchaseUnits"
        private const val UPDATE_CUSTOMER_SESSION = "updateCustomerSession"
    }
}
