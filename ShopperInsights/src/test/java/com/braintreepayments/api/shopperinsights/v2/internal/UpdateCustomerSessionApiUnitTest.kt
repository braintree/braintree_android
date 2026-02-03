package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.shopperinsights.v2.CustomerSessionRequest
import com.braintreepayments.api.shopperinsights.v2.PurchaseUnit
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException

@OptIn(ExperimentalBetaApi::class)
class UpdateCustomerSessionApiUnitTest {

    private val customerSessionRequest = CustomerSessionRequest(
        hashedEmail = "hashedEmail",
        hashedPhoneNumber = "hashedPhoneNumber",
        payPalAppInstalled = true,
        venmoAppInstalled = false,
        purchaseUnits = null
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
        }
    )

    private val sessionId = "updated-session-id"

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called and a responseBody is returned, callback with Success is invoked`() = runTest {
        val responseBody = """
            {
                "data": {
                    "updateCustomerSession": {
                        "sessionId": "$sessionId"
                    }
                }
            }
        """.trimIndent()

        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(responseBody)
            .build()

        val responseParser = mockk<ShopperInsightsResponseParser> {
            every { parseSessionId(responseBody, "updateCustomerSession") } returns sessionId
        }

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val updateCustomerSessionApi = UpdateCustomerSessionApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder>(relaxed = true),
            responseParser = responseParser,
            mainDispatcher = testDispatcher,
            coroutineScope = testScope
        )

        val callback = mockk<(UpdateCustomerSessionApi.UpdateCustomerSessionResult) -> Unit>(relaxed = true)
        updateCustomerSessionApi.execute(customerSessionRequest, sessionId, callback)
        advanceUntilIdle()

        verify { callback.invoke(UpdateCustomerSessionApi.UpdateCustomerSessionResult.Success(sessionId)) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when execute is called and an error is returned, callback with Error is invoked`() = runTest {
        val error = IOException("Network error")

        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostErrorResponse(error)
            .build()

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val updateCustomerSessionApi = UpdateCustomerSessionApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder>(relaxed = true),
            responseParser = mockk<ShopperInsightsResponseParser>(relaxed = true),
            mainDispatcher = testDispatcher,
            coroutineScope = testScope
        )

        val callback = mockk<(UpdateCustomerSessionApi.UpdateCustomerSessionResult) -> Unit>(relaxed = true)
        updateCustomerSessionApi.execute(customerSessionRequest, sessionId, callback)
        advanceUntilIdle()

        verify { callback.invoke(UpdateCustomerSessionApi.UpdateCustomerSessionResult.Error(error)) }
    }

    @Test
    fun `when execute is called and a JSONException is thrown, callback with Error is invoked`() {
        val exception = JSONException("Test exception")
        val customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder> {
            every { createRequestObjects(any()) } throws exception
        }

        val updateCustomerSessionApi = UpdateCustomerSessionApi(
            braintreeClient = mockk<BraintreeClient>(relaxed = true),
            customerSessionRequestBuilder = customerSessionRequestBuilder,
            responseParser = mockk<ShopperInsightsResponseParser>(relaxed = true)
        )

        val callback = mockk<(UpdateCustomerSessionApi.UpdateCustomerSessionResult) -> Unit>(relaxed = true)
        updateCustomerSessionApi.execute(customerSessionRequest, sessionId, callback)

        verify { callback.invoke(UpdateCustomerSessionApi.UpdateCustomerSessionResult.Error(exception)) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressWarnings("LongMethod")
    @Test
    fun `when execute is called, the correct GraphQL request body is sent`() = runTest {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        val customerSessionRequestWithPurchaseUnits = customerSessionRequest.copy(
            purchaseUnits = listOf(
                PurchaseUnit(amount = "100.00", currencyCode = "USD")
            )
        )

        val customerSessionRequestBuilder = mockk<CustomerSessionRequestBuilder> {
            every { createRequestObjects(customerSessionRequestWithPurchaseUnits) } returns expectedJsonRequestObjects
        }

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val updateCustomerSessionApi = UpdateCustomerSessionApi(
            braintreeClient = braintreeClient,
            customerSessionRequestBuilder = customerSessionRequestBuilder,
            responseParser = mockk<ShopperInsightsResponseParser>(relaxed = true),
            mainDispatcher = testDispatcher,
            coroutineScope = testScope
        )

        val expectedRequestBody = JSONObject().apply {
            put(
                "query",
                """
                mutation UpdateCustomerSession(${'$'}input: UpdateCustomerSessionInput!) {
                    updateCustomerSession(input: ${'$'}input) {
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
                        })
                        put("sessionId", sessionId)
                    })
                }
            )
        }

        val callback = mockk<(UpdateCustomerSessionApi.UpdateCustomerSessionResult) -> Unit>(relaxed = true)
        updateCustomerSessionApi.execute(customerSessionRequestWithPurchaseUnits, sessionId, callback)
        advanceUntilIdle()

        coVerify {
            braintreeClient.sendGraphQLPOST(withArg { actualRequestBody ->
                JSONAssert.assertEquals(expectedRequestBody, actualRequestBody, false)
            })
        }
    }
}
