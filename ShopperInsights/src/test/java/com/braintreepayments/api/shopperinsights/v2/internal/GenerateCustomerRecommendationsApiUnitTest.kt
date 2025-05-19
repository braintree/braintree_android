package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerRecommendationsResult
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.PaymentOptions
import com.braintreepayments.api.shopperinsights.v2.PurchaseUnit
import com.braintreepayments.api.shopperinsights.v2.internal.CustomerSessionRequestBuilder.JsonRequestObjects
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

@OptIn(ExperimentalBetaApi::class)
class GenerateCustomerRecommendationsApiUnitTest {

    private val customerSessionRequest = CustomerSessionRequest(
        hashedEmail = "hashedEmail",
        hashedPhoneNumber = "hashedPhoneNumber",
        payPalAppInstalled = true,
        venmoAppInstalled = false,
        purchaseUnits = listOf(
            PurchaseUnit(
                amount = "100.00",
                currencyCode = "USD"
            ),
            PurchaseUnit(
                amount = "200.00",
                currencyCode = "EUR"
            )
        )
    )

    private val jsonRequestObjects = JsonRequestObjects(
        customer = JSONObject().apply {
            put("hashedEmail", "hashedEmail")
            put("hashedPhoneNumber", "hashedPhoneNumber")
            put("paypalAppInstalled", true)
            put("venmoAppInstalled", false)
        },
        purchaseUnits = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("amount", JSONObject().apply {
                        put("value", "100.00")
                        put("currencyCode", "USD")
                    })
                }
            )
            put(
                JSONObject().apply {
                    put("amount", JSONObject().apply {
                        put("value", "200.00")
                        put("currencyCode", "EUR")
                    })
                }
            )
        }
    )

    private val expectedRequestBody = JSONObject().apply {
        put(
            "query",
            """
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
        put(
            "variables",
            JSONObject().apply {
                put("input", JSONObject().apply {
                    put("sessionId", "test-session-id")
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

    private lateinit var customerSessionRequestBuilder: CustomerSessionRequestBuilder
    private lateinit var callback: (GenerateCustomerRecommendationsApi.GenerateCustomerRecommendationsResult) -> Unit

    @Before
    fun setup() {
        customerSessionRequestBuilder = mockk(relaxed = true)
        callback = mockk(relaxed = true)
    }

    @Test
    fun `when execute is called and a responseBody is returned, callback with Success is invoked`() {
        val sessionId = "test-session-id"
        val responseBody = """
            {
                "data": {
                    "generateCustomerRecommendations": {
                        "sessionId": "$sessionId",
                        "isInPayPalNetwork": true,
                        "paymentRecommendations": [
                            {
                                "paymentOption": "PAYPAL",
                                "recommendedPriority": 1
                            }
                        ]
                    }
                }
            }
        """.trimIndent()

        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPOSTSuccessfulResponse(responseBody)
            .build()

        val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder
        )

        generateCustomerRecommendationsApi.execute(customerSessionRequest, "test-session-id", callback)

        val expectedResult = CustomerRecommendationsResult(
            sessionId = sessionId,
            isInPayPalNetwork = true,
            paymentRecommendations = listOf(
                PaymentOptions(paymentOption = "PAYPAL", recommendedPriority = 1)
            )
        )

        verify {
            callback.invoke(
                GenerateCustomerRecommendationsApi.GenerateCustomerRecommendationsResult.Success(expectedResult)
            )
        }
    }

    @Test
    fun `when execute is called and an error is returned, callback with Error is invoked`() {
        val error = Exception("Network error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPOSTErrorResponse(error)
            .build()
        val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder
        )

        generateCustomerRecommendationsApi.execute(customerSessionRequest, "test-session-id", callback)

        verify {
            callback.invoke(GenerateCustomerRecommendationsApi.GenerateCustomerRecommendationsResult.Error(error))
        }
    }

    @Test
    fun `when execute is called and a JSONException is thrown, callback with Error is invoked`() {
        val exception = JSONException("Test exception")
        val braintreeClient = mockk<BraintreeClient> {
            every { sendGraphQLPOST(any(), any()) } throws exception
        }
        val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder
        )

        generateCustomerRecommendationsApi.execute(customerSessionRequest, "test-session-id", callback)

        verify {
            callback.invoke(
                GenerateCustomerRecommendationsApi.GenerateCustomerRecommendationsResult.Error(exception)
            )
        }
    }

    @Test
    fun `when execute is called, the correct GraphQL request body is sent`() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { customerSessionRequestBuilder.createRequestObjects(customerSessionRequest) } returns jsonRequestObjects

        val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder,
        )

        generateCustomerRecommendationsApi.execute(
            customerSessionRequest = customerSessionRequest,
            sessionId = "test-session-id",
            callback = mockk(relaxed = true)
        )

        verify {
            braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                JSONAssert.assertEquals(expectedRequestBody, actualRequestBody, false)
            }, any())
        }
    }
}
