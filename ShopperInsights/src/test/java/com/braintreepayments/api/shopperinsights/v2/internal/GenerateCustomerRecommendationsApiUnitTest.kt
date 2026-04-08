package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerRecommendations
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.PayPalCampaign
import com.braintreepayments.api.shopperinsights.v2.PaymentOptions
import com.braintreepayments.api.shopperinsights.v2.PurchaseUnit
import com.braintreepayments.api.shopperinsights.v2.internal.GenerateCustomerRecommendationsApi.GenerateCustomerRecommendationsResult
import com.braintreepayments.api.shopperinsights.v2.internal.CustomerSessionRequestBuilder.JsonRequestObjects
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException
import kotlin.test.assertEquals

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
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        customerSessionRequestBuilder = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called and a responseBody is returned, callback with Success is invoked`() =
        runTest(testDispatcher) {
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
                .sendGraphQLPostSuccessfulResponse(responseBody)
                .build()

            val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
                braintreeClient = braintreeClient,
                customerSessionRequestBuilder = customerSessionRequestBuilder
            )

            val result = generateCustomerRecommendationsApi.execute(customerSessionRequest, "test-session-id")
            advanceUntilIdle()

            val expectedResult = CustomerRecommendations(
                sessionId = sessionId,
                isInPayPalNetwork = true,
                paymentRecommendations = listOf(
                    PaymentOptions(paymentOption = "PAYPAL", recommendedPriority = 1)
                )
            )

            assert(result is GenerateCustomerRecommendationsResult.Success)
            assertEquals(expectedResult, (result as GenerateCustomerRecommendationsResult.Success).customerRecommendations)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called and an error is returned, callback with Error is invoked`() = runTest(testDispatcher) {
        val error = IOException("Network error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostErrorResponse(error)
            .build()

        val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder
        )

        val result = generateCustomerRecommendationsApi.execute(customerSessionRequest, "test-session-id")
        advanceUntilIdle()

        assert(result is GenerateCustomerRecommendationsResult.Error)
        assertEquals(error, (result as GenerateCustomerRecommendationsResult.Error).error)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called and a JSONException is thrown, callback with Error is invoked`() =
        runTest(testDispatcher) {
            val exception = JSONException("Test exception")
            val braintreeClient = mockk<BraintreeClient> {
                coEvery { sendGraphQLPOST(any()) } throws exception
            }

            val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
                braintreeClient = braintreeClient,
                customerSessionRequestBuilder = customerSessionRequestBuilder
            )

            val result = generateCustomerRecommendationsApi.execute(customerSessionRequest, "test-session-id")
            advanceUntilIdle()

            assert(result is GenerateCustomerRecommendationsResult.Error)
            assertEquals(exception, (result as GenerateCustomerRecommendationsResult.Error).error)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called, the correct GraphQL request body is sent`() = runTest(testDispatcher) {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { customerSessionRequestBuilder.createRequestObjects(customerSessionRequest) } returns jsonRequestObjects

        val generateCustomerRecommendationsApi = GenerateCustomerRecommendationsApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder
        )

        generateCustomerRecommendationsApi.execute(
            customerSessionRequest = customerSessionRequest,
            sessionId = "test-session-id"
        )
        advanceUntilIdle()

        coVerify {
            braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                JSONAssert.assertEquals(expectedRequestBody, actualRequestBody, false)
            })
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called with sessionId and payPalCampaigns only, GraphQL variables include campaigns`() =
        runTest(testDispatcher) {
            val braintreeClient = mockk<BraintreeClient>(relaxed = true)
            val realBuilder = CustomerSessionRequestBuilder()
            val generateApi = GenerateCustomerRecommendationsApi(
                braintreeClient = braintreeClient,
                customerSessionRequestBuilder = realBuilder
            )

            generateApi.execute(
                customerSessionRequest = null,
                sessionId = "test-session-id",
                payPalCampaigns = listOf(PayPalCampaign(id = "RECOMMENDED_OFFER_1"))
            )
            advanceUntilIdle()

            val expectedBody = JSONObject().apply {
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
                        put(
                            "input",
                            JSONObject().apply {
                                put("sessionId", "test-session-id")
                                put(
                                    "paypal_campaigns",
                                    JSONArray().apply {
                                        put(JSONObject().put("id", "RECOMMENDED_OFFER_1"))
                                    }
                                )
                            }
                        )
                    }
                )
            }

            coVerify {
                braintreeClient.sendGraphQLPOST(withArg { actual ->
                    JSONAssert.assertEquals(expectedBody, actual, false)
                })
            }
        }
}
