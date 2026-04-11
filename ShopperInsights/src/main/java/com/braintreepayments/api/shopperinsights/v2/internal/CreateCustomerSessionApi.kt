package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import org.json.JSONException
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

/**
 * API to create a new customer session using the `CreateCustomerSession` GraphQL mutation.
 */
@ExperimentalBetaApi
@Suppress("TooGenericExceptionCaught")
internal class CreateCustomerSessionApi(
    private val braintreeClient: BraintreeClient,
    private val customerSessionRequestBuilder: CustomerSessionRequestBuilder = CustomerSessionRequestBuilder(),
    private val responseParser: ShopperInsightsResponseParser = ShopperInsightsResponseParser()
) {

    sealed class CreateCustomerSessionResult {
        data class Success(val sessionId: String) : CreateCustomerSessionResult()
        data class Error(val error: Exception) : CreateCustomerSessionResult()
    }

    suspend fun execute(customerSessionRequest: CustomerSessionRequest): CreateCustomerSessionResult {
        return try {
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

            try {
                val responseBody = braintreeClient.sendGraphQLPOST(params)
                val sessionId = responseParser.parseSessionId(responseBody, CREATE_CUSTOMER_SESSION)
                CreateCustomerSessionResult.Success(sessionId)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                CreateCustomerSessionResult.Error(e)
            }
        } catch (e: JSONException) {
            CreateCustomerSessionResult.Error(e)
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
