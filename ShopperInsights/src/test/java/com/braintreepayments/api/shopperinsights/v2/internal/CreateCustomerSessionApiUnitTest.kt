package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.PurchaseUnit
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException

@OptIn(ExperimentalBetaApi::class)
class CreateCustomerSessionApiUnitTest {

    private val customerSessionRequest = CustomerSessionRequest(
        hashedEmail = "hashedEmail",
        hashedPhoneNumber = "hashedPhoneNumber",
        payPalAppInstalled = true,
        venmoAppInstalled = false,
        purchaseUnits = null
    )

    private val customerSessionRequestWithPurchaseUnits = customerSessionRequest.copy(
        purchaseUnits = listOf(
            PurchaseUnit(amount = "100.00", currencyCode = "USD"),
            PurchaseUnit(amount = "200.00", currencyCode = "EUR")
        )
    )

    private val expectedJsonRequestObjects = CustomerSessionRequestBuilder.JsonRequestObjects(
        customer = JSONObject().apply {
            put("hashedEmail", "hashedEmail")
            put("hashedPhoneNumber", "hashedPhoneNumber")
            put("paypalAppInstalled", true)
            put("venmoAppInstalled", false)
        },
        purchaseUnits = JSONArray().apply {
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
        }
    )

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope
    lateinit var braintreeClient: BraintreeClient

    @Before
    fun setup() {
        braintreeClient = mockk<BraintreeClient>(relaxed = true)
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
                    "createCustomerSession": {
                        "sessionId": "$sessionId"
                    }
                }
            }
        """.trimIndent()

        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(responseBody)
            .build()

        val customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder>(relaxed = true)
        val responseParser = mockk<ShopperInsightsResponseParser> {
            every { parseSessionId(responseBody, "createCustomerSession") } returns sessionId
        }

        val createCustomerSessionApi = CreateCustomerSessionApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder,
            responseParser = responseParser,
            mainDispatcher = testDispatcher,
            coroutineScope = testScope
        )

        val callback = mockk<(CreateCustomerSessionApi.CreateCustomerSessionResult) -> Unit>(relaxed = true)
        createCustomerSessionApi.execute(customerSessionRequest, callback)
        advanceUntilIdle()

        verify { callback.invoke(CreateCustomerSessionApi.CreateCustomerSessionResult.Success(sessionId)) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called and an error is returned, callback with Error is invoked`() =
    runTest(testDispatcher) {
        val error = IOException("Network error")

        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostErrorResponse(error)
            .build()

        val customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder>(relaxed = true)
        val responseParser = mockk<ShopperInsightsResponseParser>(relaxed = true)

        val createCustomerSessionApi = CreateCustomerSessionApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder,
            responseParser = responseParser,
            mainDispatcher = testDispatcher,
            coroutineScope = testScope
        )

        val callback = mockk<(CreateCustomerSessionApi.CreateCustomerSessionResult) -> Unit>(relaxed = true)
        createCustomerSessionApi.execute(customerSessionRequest, callback)
        advanceUntilIdle()

        verify { callback.invoke(CreateCustomerSessionApi.CreateCustomerSessionResult.Error(error)) }
    }

    @Test
    fun `when execute is called and a JSONException is thrown, callback with Error is invoked`() {
        val exception = JSONException("Test exception")
        val customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder> {
            every { createRequestObjects(any()) } throws exception
        }
        val responseParser = mockk<ShopperInsightsResponseParser>(relaxed = true)

        val createCustomerSessionApi = CreateCustomerSessionApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder,
            responseParser = responseParser
        )

        val callback = mockk<(CreateCustomerSessionApi.CreateCustomerSessionResult) -> Unit>(relaxed = true)
        createCustomerSessionApi.execute(customerSessionRequest, callback)

        verify { callback.invoke(CreateCustomerSessionApi.CreateCustomerSessionResult.Error(exception)) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressWarnings("LongMethod")
    @Test
    fun `when execute is called, the correct GraphQL request body is sent`() = runTest(testDispatcher) {
        val customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder> {
            every { createRequestObjects(customerSessionRequestWithPurchaseUnits) } returns expectedJsonRequestObjects
        }

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

        val createCustomerSessionApi = CreateCustomerSessionApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder,
            responseParser = mockk<ShopperInsightsResponseParser>(relaxed = true),
            mainDispatcher = testDispatcher,
            coroutineScope = testScope
        )

        createCustomerSessionApi.execute(customerSessionRequestWithPurchaseUnits, mockk(relaxed = true))
        advanceUntilIdle()

        coVerify {
            braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                JSONAssert.assertEquals(expectedRequestBody, actualRequestBody, false)
            })
        }
    }
}
