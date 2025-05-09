package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * API to create a new customer session using the `CreateCustomerSession` GraphQL mutation.
 */
@ExperimentalBetaApi
internal class CreateCustomerSessionApi(
    private val braintreeClient: BraintreeClient,
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
                    callback(CreateCustomerSessionResult.Success(parseSessionId(responseBody)))
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
        val customer = JSONObject().apply {
            putOpt(HASHED_EMAIL, customerSessionRequest.hashedEmail)
            putOpt(HASHED_PHONE_NUMBER, customerSessionRequest.hashedPhoneNumber)
            putOpt(PAYPAL_APP_INSTALLED, customerSessionRequest.payPalAppInstalled)
            putOpt(VENMO_APP_INSTALLED, customerSessionRequest.venmoAppInstalled)
        }

        val purchaseUnits = customerSessionRequest.purchaseUnits
            ?.takeIf { it.isNotEmpty() }
            ?.let { purchaseUnits ->
                JSONArray().apply {
                    purchaseUnits.forEach { purchaseUnit ->
                        put(
                            JSONObject().put(
                                AMOUNT,
                                JSONObject().apply {
                                    put(VALUE, purchaseUnit.amount)
                                    put(CURRENCY_CODE, purchaseUnit.currencyCode)
                                }
                            )
                        )
                    }
                }
            }

        val input = JSONObject().apply {
            put(CUSTOMER, customer)
            putOpt(PURCHASE_UNITS, purchaseUnits)
        }

        return JSONObject().put(INPUT, input)
    }

    @Throws(JSONException::class)
    private fun parseSessionId(responseBody: String): String {
        val data = JSONObject(responseBody).getJSONObject(DATA)
        val createCustomerSession = data.getJSONObject(CREATE_CUSTOMER_SESSION)
        return createCustomerSession.getString(SESSION_ID)
    }

    companion object {
        private const val QUERY = "query"
        private const val VARIABLES = "variables"
        private const val INPUT = "input"
        private const val CUSTOMER = "customer"
        private const val HASHED_EMAIL = "hashedEmail"
        private const val HASHED_PHONE_NUMBER = "hashedPhoneNumber"
        private const val PAYPAL_APP_INSTALLED = "paypalAppInstalled"
        private const val VENMO_APP_INSTALLED = "venmoAppInstalled"
        private const val PURCHASE_UNITS = "purchaseUnits"
        private const val AMOUNT = "amount"
        private const val VALUE = "value"
        private const val CURRENCY_CODE = "currencyCode"
        private const val DATA = "data"
        private const val CREATE_CUSTOMER_SESSION = "createCustomerSession"
        private const val SESSION_ID = "sessionId"
    }
}
