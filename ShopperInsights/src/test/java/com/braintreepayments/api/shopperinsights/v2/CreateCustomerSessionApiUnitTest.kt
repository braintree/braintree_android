package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

@OptIn(ExperimentalBetaApi::class)
class CreateCustomerSessionApiUnitTest {

    private val customerSessionRequest = CustomerSessionRequest(
        hashedEmail = "hashedEmail",
        hashedPhoneNumber = "hashedPhoneNumber",
        payPalAppInstalled = true,
        venmoAppInstalled = false,
        purchaseUnits = null
    )

    @Test
    fun `when execute is called and a responseBody is returned, callback with Success is invoked`() {
        val sessionId = "test-session-id"
        val responseBody = """
            {
                "data": {
                    "createCustomerSession": {
                        "sessionId": "$sessionId"
                    }
                }
            }
        """.trimIndent()

        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPOSTSuccessfulResponse(responseBody)
            .build()

        val createCustomerSessionApi = CreateCustomerSessionApi(braintreeClient)

        val callback = mockk<(CreateCustomerSessionApi.CreateCustomerSessionResult) -> Unit>(relaxed = true)
        createCustomerSessionApi.execute(customerSessionRequest, callback)

        verify { callback.invoke(CreateCustomerSessionApi.CreateCustomerSessionResult.Success(sessionId)) }
    }

    @Test
    fun `when execute is called and an error is returned, callback with Error is invoked`() {
        val error = Exception("Network error")

        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPOSTErrorResponse(error)
            .build()

        val createCustomerSessionApi = CreateCustomerSessionApi(braintreeClient)

        val callback = mockk<(CreateCustomerSessionApi.CreateCustomerSessionResult) -> Unit>(relaxed = true)
        createCustomerSessionApi.execute(customerSessionRequest, callback)

        verify { callback.invoke(CreateCustomerSessionApi.CreateCustomerSessionResult.Error(error)) }
    }

    @Test
    fun `when execute is called and a JSONException is thrown, callback with Error is invoked`() {
        val exception = JSONException("Test exception")
        val createCustomerSessionApi = CreateCustomerSessionApi(mockk())
        val callback = mockk<(CreateCustomerSessionApi.CreateCustomerSessionResult) -> Unit>(relaxed = true)
        val customerSessionRequest = mockk<CustomerSessionRequest>(relaxed = true)
        every { customerSessionRequest.hashedEmail } throws exception

        createCustomerSessionApi.execute(customerSessionRequest, callback)

        verify { callback.invoke(CreateCustomerSessionApi.CreateCustomerSessionResult.Error(exception)) }
    }

    @Test
    fun `when execute is called, the correct GraphQL request body is sent`() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        val createCustomerSessionApi = CreateCustomerSessionApi(braintreeClient)

        val customerSessionRequestWithPurchaseUnits = customerSessionRequest.copy(
            purchaseUnits = listOf(
                PurchaseUnit(amount = "100.00", currencyCode = "USD"),
                PurchaseUnit(amount = "200.00", currencyCode = "EUR")
            )
        )

        val expectedRequestBody = JSONObject().apply {
            put(
                "query",
                """
                mutation CreateCustomerSession(${'$'}input: CreateCustomerSessionInput!) {
                    createCustomerSession(input: ${'$'}input) {
                        sessionId
                    }
                }
                """.trimIndent()
            )
            put(
                "variables",
                JSONObject().apply {
                    put("input", JSONObject().apply {
                        put("customer", JSONObject().apply {
                            put("hashedEmail", "hashedEmail")
                            put("hashedPhoneNumber", "hashedPhoneNumber")
                            put("paypalAppInstalled", true)
                            put("venmoAppInstalled", false)
                        })
                        put("purchaseUnits", JSONArray().apply {
                            put(JSONObject().apply {
                                put("amount", JSONObject().apply {
                                    put("value", "100.00")
                                    put("currencyCode", "USD")
                                })
                            })
                            put(JSONObject().apply {
                                put("amount", JSONObject().apply {
                                    put("value", "200.00")
                                    put("currencyCode", "EUR")
                                })
                            })
                        })
                    })
                }
            )
        }

        val callback = mockk<(CreateCustomerSessionApi.CreateCustomerSessionResult) -> Unit>(relaxed = true)
        createCustomerSessionApi.execute(customerSessionRequestWithPurchaseUnits, callback)

        verify {
            braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                JSONAssert.assertEquals(expectedRequestBody, actualRequestBody, false)
            }, any())
        }
    }
}
