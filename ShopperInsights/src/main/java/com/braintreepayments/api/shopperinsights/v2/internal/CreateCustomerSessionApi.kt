package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import org.json.JSONException
import org.json.JSONObject

/**
 * API to create a new customer session using the `CreateCustomerSession` GraphQL mutation.
 */
@ExperimentalBetaApi
internal class CreateCustomerSessionApi(
    private val braintreeClient: BraintreeClient,
    private val customerSessionRequestBuilder: CustomerSessionRequestBuilder = CustomerSessionRequestBuilder(),
    private val responseParser: ShopperInsightsResponseParser = ShopperInsightsResponseParser()
) {

    sealed class CreateCustomerSessionResult {
        data class Success(val sessionId: String) : CreateCustomerSessionResult()
        data class Error(val error: Exception) : CreateCustomerSessionResult()
    }

    fun execute(
        customerSessionRequest: CustomerSessionRequest,
        callback: (CreateCustomerSessionResult) -> Unit
    ) {
        try {
            val params = JSONObject()
            params.put(
                QUERY, """
                mutation CreateCustomerSession(${'$'}input: CreateCustomerSessionInput!) {
                    createCustomerSession(input: ${'$'}input) {
                        sessionId
                    }
                }
                """.trimIndent()
            )

            params.put(VARIABLES, assembleVariables(customerSessionRequest))

            braintreeClient.sendGraphQLPOST(params) { responseBody: String?, httpError: Exception? ->
                if (responseBody != null) {
                    val sessionId = responseParser.parseSessionId(responseBody, CREATE_CUSTOMER_SESSION)
                    callback(
                        CreateCustomerSessionResult.Success(sessionId)
                    )
                } else if (httpError != null) {
                    callback(CreateCustomerSessionResult.Error(httpError))
                }
            }
        } catch (e: JSONException) {
            callback(CreateCustomerSessionResult.Error(e))
        }
    }

    @Throws(JSONException::class)
    private fun assembleVariables(customerSessionRequest: CustomerSessionRequest): JSONObject {
        val jsonRequestObjects = customerSessionRequestBuilder.createRequestObjects(customerSessionRequest)

        val input = JSONObject().apply {
            put(CUSTOMER, jsonRequestObjects.customer)
            putOpt(PURCHASE_UNITS, jsonRequestObjects.purchaseUnits)
        }

        return JSONObject().put(INPUT, input)
    }

    companion object {
        private const val QUERY = "query"
        private const val VARIABLES = "variables"
        private const val INPUT = "input"
        private const val CUSTOMER = "customer"
        private const val PURCHASE_UNITS = "purchaseUnits"
        private const val CREATE_CUSTOMER_SESSION = "createCustomerSession"
    }
}
