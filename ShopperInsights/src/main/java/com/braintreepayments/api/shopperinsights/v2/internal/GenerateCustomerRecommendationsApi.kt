package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerRecommendations
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.PaymentOptions
import org.json.JSONException
import org.json.JSONObject

/**
 * API to return customer recommendations using the `GenerateCustomerRecommendations` GraphQL mutation.
 */
@ExperimentalBetaApi
internal class GenerateCustomerRecommendationsApi(
    private val braintreeClient: BraintreeClient,
    private val customerSessionRequestBuilder: CustomerSessionRequestBuilder = CustomerSessionRequestBuilder(),
) {

    sealed class GenerateCustomerRecommendationsResult {
        data class Success(
            val customerRecommendations: CustomerRecommendations
        ) : GenerateCustomerRecommendationsResult()

        data class Error(val error: Exception) : GenerateCustomerRecommendationsResult()
    }

    fun execute(
        customerSessionRequest: CustomerSessionRequest?,
        sessionId: String?,
        callback: (GenerateCustomerRecommendationsResult) -> Unit
    ) {
        try {
            val params = JSONObject()
            params.put(
                QUERY, """
                mutation GenerateCustomerRecommendations(${'$'}input: GenerateCustomerRecommendationsInput!) {
                    generateCustomerRecommendations(input: ${'$'}input) {
                        sessionId
                        isInPayPalNetwork
                        paymentRecommendations {
                            paymentOption
                            recommendedPriority
                        }
                    }
                }
                """.trimIndent()
            )

            params.put(VARIABLES, assembleVariables(sessionId, customerSessionRequest))

            braintreeClient.sendGraphQLPOST(params) { responseBody: String?, httpError: Exception? ->
                if (responseBody != null) {
                    val recommendationsResult = parseRecommendationsResponse(responseBody)
                    callback(GenerateCustomerRecommendationsResult.Success(recommendationsResult))
                } else if (httpError != null) {
                    callback(GenerateCustomerRecommendationsResult.Error(httpError))
                }
            }
        } catch (e: JSONException) {
            callback(GenerateCustomerRecommendationsResult.Error(e))
        }
    }

    @Throws(JSONException::class)
    private fun assembleVariables(
        sessionId: String?,
        customerSessionRequest: CustomerSessionRequest?
    ): JSONObject {
        val input = JSONObject().apply {
            putOpt(SESSION_ID, sessionId)

            if (customerSessionRequest != null) {
                val jsonRequestObjects = customerSessionRequestBuilder.createRequestObjects(customerSessionRequest)
                put(CUSTOMER, jsonRequestObjects.customer)
                putOpt(PURCHASE_UNITS, jsonRequestObjects.purchaseUnits)
            }
        }

        return JSONObject().put(INPUT, input)
    }

    @Throws(JSONException::class)
    private fun parseRecommendationsResponse(responseBody: String): CustomerRecommendations {
        val jsonObject = JSONObject(responseBody)
        val data = jsonObject.getJSONObject("data")
        val recommendations = data.getJSONObject(GENERATE_CUSTOMER_RECOMMENDATIONS)

        val sessionId = recommendations.getString(SESSION_ID)
        val isInPayPalNetwork = recommendations.getBoolean("isInPayPalNetwork")
        val paymentRecommendations = recommendations.getJSONArray("paymentRecommendations")

        val paymentOptions = mutableListOf<PaymentOptions>()
        for (i in 0 until paymentRecommendations.length()) {
            val recommendation = paymentRecommendations.getJSONObject(i)
            paymentOptions.add(
                PaymentOptions(
                    paymentOption = recommendation.getString("paymentOption"),
                    recommendedPriority = recommendation.getInt("recommendedPriority")
                )
            )
        }

        return CustomerRecommendations(
            sessionId = sessionId,
            isInPayPalNetwork = isInPayPalNetwork,
            paymentRecommendations = paymentOptions
        )
    }

    companion object {
        private const val QUERY = "query"
        private const val VARIABLES = "variables"
        private const val INPUT = "input"
        private const val SESSION_ID = "sessionId"
        private const val CUSTOMER = "customer"
        private const val PURCHASE_UNITS = "purchaseUnits"
        private const val GENERATE_CUSTOMER_RECOMMENDATIONS = "generateCustomerRecommendations"
    }
}
